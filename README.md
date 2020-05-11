# TcpApp
Android Tcp 封装

# gradle 引用tcp模块
implementation 'com.github.kingskys:TcpApp:v1.1.1'

<h1>TcpClient数据格式</h1>

数据头 = "hot" + 数据体长度 + "ing"
数据体 = 数据体数据

读取时，先读10个字节的数据头，匹配是不是 ("hot" + 数据体长度 + "ing") 的格式，然后拿到数据体长度，再读取数据体
写入时，同理 ("hot" + 数据体长度 + "ing" + 数据体数据)

<h1>TcpWrapper封装</h1>
具体使用参考app里面的代码
