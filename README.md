<h1 align="center">
  :beetle: TCP - UDP - Rest :beetle:
</h1>

<h2 align="center">
  Server to analyze packet transfer 
</h2>

<p align="center">
  <img alt="Made by Valney Marinho" src="https://img.shields.io/badge/made%20by-valn3y-%20?color=7159C1">
</p>

## :rocket: Build and run with
``` bash
./mvnw spring-boot:run
```

## ❓ How to check if servers are running?
TCP
``` bash
netstat -tulnp | grep 9000
```
UDP
``` bash
netstat -ulnp | grep 9001
```

## ❓ How to connect and send commands?
TCP
``` bash
nc localhost 9000
2|MB
```
UDP
``` bash
echo "2|MB" | nc -u -w1 localhost 9001
```

## ❓ How to check Rest API?
```
http://localhost:8080/swagger-ui/index.html
```

## :mailbox_with_mail: Contact me!
<div align="center">
<a href="https://www.linkedin.com/in/valney-júnior-b34384149"><img alt="LinkedIn" src="https://img.shields.io/badge/linkedin%20-%230077B5.svg?&style=for-the-badge&logo=linkedin"/></a> &nbsp;
<a href="mailto:neymarinho.junior@gmail.com"><img alt="Gmail" src="https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white" /></a> &nbsp;
</div>
