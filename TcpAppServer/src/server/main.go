package main

import (
	"bufio"
	"encoding/base64"
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"
	"utils"

	"server/msgtype"
)

var (
	patternL = [3]byte{'h', 'o', 't'}
	patternR = [3]byte{'i', 'n', 'g'}
)

// 匹配数据头
// 返回数据体长度和错误
func matchHeadPattern(buf []byte) (int, error) {
	length := len(patternL)
	for i := 0; i < length; i++ {
		if buf[i] != patternL[i] {
			return i, errors.New(fmt.Sprintf("读取数据头第（%d）个字符错误，获取到：%d，应为：%d", i, buf[i], patternL))
		}
	}

	idx := length + 4
	length = len(patternR)
	for i := 0; i < length; i++ {
		if buf[i+idx] != patternR[i] {
			return idx + i, errors.New(fmt.Sprintf("读取数据头第（%d）个字符错误，获取到：%d，应为：%d", idx, buf[i+idx], patternL))
		}
	}

	realLen := binary.BigEndian.Uint32(buf[idx-4 : idx])

	return int(realLen), nil
}

func readHead(conn net.Conn, dataLen int) (int, error) {
	var buf = make([]byte, dataLen)
	err := read(conn, buf)
	if err != nil {
		if opErr, ok := err.(*net.OpError); ok {
			if opErr.Timeout() {
				log.Println("等待超时，断开与客户端连接")
				return 0, errors.New(fmt.Sprint("等待客户端输入超时"))
			}
		}

		if err == io.EOF {
			return 0, errors.New(fmt.Sprint("客户端断开连接"))
		} else {
			fmt.Printf("错误类型:%T\n", err)
			return 0, errors.New(fmt.Sprintf("读取数据头错误：%v\n", err))
		}
	} else {
		n, err := matchHeadPattern(buf)
		if err != nil {
			return n, err
		}

		return n, nil
	}
}

func read(conn net.Conn, buf []byte) error {
	totalLen := len(buf)
	readLen := 0
	for readLen < totalLen {
		n, err := conn.Read(buf[readLen:])
		if err != nil {
			return err
		}

		readLen += n
	}

	return nil
}

func makeReadBodyErr(err error) error {
	if err != nil {
		if opErr, ok := err.(*net.OpError); ok {
			if opErr.Timeout() {
				log.Println("等待数据体超时")
				return errors.New(fmt.Sprint("等待客户端传入数据超时"))
			}
		}

		if err == io.EOF {
			return errors.New(fmt.Sprint("客户端断开连接"))
		} else {
			return errors.New(fmt.Sprintf("读取数据体错误：%v\n", err))
		}
	}

	return err
}

func readBody(conn net.Conn, dataLen int) ([]byte, error) {
	var buf = make([]byte, dataLen)
	err := read(conn, buf)
	if err != nil {
		err = makeReadBodyErr(err)
		return nil, err
	}

	return buf, nil

}

var useBase64 = false

func convertBody(data []byte) ([]byte, error) {
	if useBase64 {
		length := base64.StdEncoding.DecodedLen(len(data))
		buf := make([]byte, length)
		n, err := base64.StdEncoding.Decode(buf, data)
		if err != nil {
			return nil, err
		}
		return buf[:n], nil
	}
	return data, nil
}

// 设置等待客户端输入30秒超时
func updateReadHeadTimeout(conn net.Conn) {
	conn.SetReadDeadline(time.Now().Add(time.Second * 30))
}

// 设置读取数据体超时
func updateReadBodyTimeout(conn net.Conn, dataLen int) {
	minTime := time.Second * 3
	needTime := time.Millisecond * time.Duration(dataLen) * 2 // 500字节/秒
	if needTime < minTime {
		needTime = minTime
	}
	go log.Printf("读取数据体超时时间为：%v\n", needTime)
	conn.SetReadDeadline(time.Now().Add(needTime))
}

func process(conn net.Conn) {
	fmt.Println("与客户端进入了连接")
	defer conn.Close()
	defer fmt.Println("与客户端断开了连接")
	headLen := len(patternL) + len(patternR) + 4

	for {
		updateReadHeadTimeout(conn)
		n, err := readHead(conn, headLen)
		if err != nil {
			fmt.Println(err)
			return
		}

		go log.Printf("解析数据头成功，数据体长度为：%d\n", n)
		if n == 0 {
			continue
		}

		updateReadBodyTimeout(conn, n)
		data, err := readBody(conn, n)
		if err != nil {
			go log.Println(err)
			return
		}

		go log.Println("读取数据体结束")

		data, err = convertBody(data)
		if err != nil {
			go log.Printf("解析数据体出错：%v\n", err)
			responseErrData(conn, errors.New(fmt.Sprintf("解析数据体出错：%v\n", err)))
		} else {
			go log.Printf("解析数据体成功\n")
			// fmt.Printf("数据体为：%v\n", string(data))
			// responseData(conn, data)
			processMain(conn, data)
		}

	}
}

func processMain(conn net.Conn, data []byte) {
	t := msgtype.MsgType(data[0])
	go log.Printf("接收的数据格式为：%v\n", t)
	switch t {
	case msgtype.NilType:
	case msgtype.NormalType:
		processNormal(conn, data)
	case msgtype.JsonType:
		processJson(conn, data)
	case msgtype.FileType:
		processFile(conn, data)
	default:
	}
}

func processNormal(conn net.Conn, data []byte) {
	fmt.Printf("数据体为：%v\n", string(data[1:]))
	responseData(conn, data)
}

func processJson(conn net.Conn, data []byte) {
	processNormal(conn, data)
}

var lock sync.Mutex
var i_idx = 0

func nextIdx() int {
	lock.Lock()
	defer lock.Unlock()

	i_idx++
	return i_idx
}

func processFile(conn net.Conn, data []byte) {
	defer responseData(conn, data)

	buf := data[1:]
	length := int(binary.BigEndian.Uint32(buf[:4]))
	buf = buf[4:]
	if length < 1 || length > len(buf) {
		fmt.Printf("文件名长度错误，length = %d，剩余数据长度 = %d\n", length, len(buf))
		return
	}
	fileName := string(buf[:length])
	fmt.Printf("文件名为：%v\n", fileName)

	fileName = strconv.Itoa(nextIdx()) + "_" + fileName
	fmt.Printf("改文件名为：%v\n", fileName)

	buf = buf[length:]
	fmt.Printf("文件长度为：%v\n", len(buf))

	dir := filepath.Join(getLocalDir(), "res", "file")

	if err := os.MkdirAll(dir, 0777); err != nil {
		fmt.Printf("创建文件目录失败：%v\n", err)
	}
	fpath := filepath.Join(dir, fileName)
	go saveFile(fpath, buf)
}

func getLocalDir() string {
	dir, err := filepath.Abs(filepath.Dir(os.Args[0]))
	if err != nil {
		log.Fatal(err)
	}
	return strings.Replace(dir, "\\", "/", -1)
}

func saveFile(fpath string, data []byte) {
	go log.Printf("开始保存文件：%s\n", fpath)
	f, err := os.OpenFile(fpath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0666)
	if err != nil {
		go log.Printf("打开文件错误：%v\n", err)
		return
	}
	defer f.Close()

	writer := bufio.NewWriter(f)
	n, err := writer.Write(data)
	if err != nil {
		go log.Printf("保存文件失败：%v\n", err)
		return
	}

	if n != len(data) {
		go log.Printf("保存文件不完整，长度应为：%d，实际长度：%d\n", len(data), n)
	} else {
		go log.Printf("保存文件成功，文件地址：%s\n", fpath)
	}

}

func responseData(conn net.Conn, data []byte) {
	// data := `{"status":1, "msg":"成功"}`
	sendData(conn, []byte(data))
}

func responseErrData(conn net.Conn, err error) {
	data := `{"status":-1, "msg":"%s"}`
	data = fmt.Sprintf(data, err.Error())
	sendData(conn, []byte(data))
}

func sendData(conn net.Conn, data []byte) {
	go log.Println("向客户端发送数据")
	data = encodeSendData(data)
	// fmt.Printf("encode以后 data = %v\n", data)
	data = formatSendData(data)
	// fmt.Println("数据 - ", data)
	_, err := conn.Write(data)
	if err != nil {
		fmt.Printf("发送数据错误：%v\n", err)
	} else {
		fmt.Println("发送数据成功")
	}
}

func encodeSendData(data []byte) []byte {
	if useBase64 {
		n := base64.StdEncoding.EncodedLen(len(data))
		dst := make([]byte, n)
		base64.StdEncoding.Encode(dst, data)
		return dst
	}
	return data
}

func formatSendData(data []byte) []byte {
	lenIdx := len(patternL)
	headLen := len(patternL) + len(patternR) + 4
	bodyLen := len(data)
	buf := make([]byte, headLen+bodyLen)

	// 数据头写入左匹配
	for i := len(patternL) - 1; i >= 0; i-- {
		buf[i] = patternL[i]
	}

	// 数据头写入右匹配
	idx := lenIdx + 4
	for i := len(patternL) - 1; i >= 0; i-- {
		buf[i+idx] = patternR[i]
	}

	// 写入数据体长度
	binary.BigEndian.PutUint32(buf[lenIdx:lenIdx+4], uint32(bodyLen))

	// 写入数据体
	for i, v := range data {
		buf[headLen+i] = v
	}

	return buf
}

func startServer() {
	port := 11001
	ipAddr := utils.GetLocalIp()
	ipPort := strconv.Itoa(port)
	addr := ipAddr + ":" + ipPort

	// addr := "0.0.0.0" + ":" + strconv.Itoa(port)
	// fmt.Printf("ip为：\n%s\n", utils.GetLocalIp())

	fmt.Printf("addr = \n%v\n", addr)

	listener, err := net.Listen("tcp", addr)
	if err != nil {
		fmt.Printf("启动服务器失败，失败原因：%v\n", err)
		return
	}

	defer listener.Close()

	for {
		fmt.Println("开始监听客户端连接...")
		conn, err := listener.Accept()
		if err != nil {
			fmt.Printf("服务器监听报错，错误：%v\n", err)
			return
		}

		go process(conn)
	}
}

func main() {
	fmt.Println("服务器启动。。。")
	defer fmt.Println("服务器已关闭")

	startServer()

}
