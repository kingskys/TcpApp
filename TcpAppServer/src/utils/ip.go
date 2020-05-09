package utils

import (
	"fmt"
	"net"
)

func GetLocalIp() string {
	ifaces, err := net.Interfaces()
	if err != nil {
		fmt.Println("net.Interfaces error = ", err)
		return ""
	}

	for _, iface := range ifaces {
		if iface.Flags&net.FlagUp == 0 {
			continue
		}

		if iface.Flags&net.FlagLoopback != 0 {
			continue
		}

		addrs, err := iface.Addrs()
		if err != nil {
			fmt.Println("get addrs error = ", err)
			return ""
		}

		for _, address := range addrs {
			if ipnet, ok := address.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
				if ipnet.IP.To4() != nil {
					return ipnet.IP.To4().String()
				}
			}
		}
	}

	return ""
}
