package msgtype

import (
	"fmt"
)

type MsgType int

func (this MsgType) String() string {
	switch this {
	case NilType:
		return "空的数据格式"
	case NormalType:
		return "普通的数据格式"
	case JsonType:
		return "json数据格式"
	case FileType:
		return "文件数据格式"
	default:
		return fmt.Sprintf("异常数据格式：%d", this)
	}
}

const (
	NilType = iota

	// 普通数据
	NormalType

	// json字符串
	JsonType

	// 文件类型
	FileType
)
