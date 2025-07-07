# Java 内存马学习项目

相信不少同学已经接触过所谓的“一句话木马”或者“webshell”。攻击者通过将这些恶意代码植入服务器的文件系统中，实现对服务器的远程控制。

然而，随着安全防护技术的发展，传统的文件型WebShell越来越难以逃避检测和清理。因此，攻击者开始植入一种无文件落地的木马——内存马（Memory Shell）。

内存马是一种将恶意代码直接注入到应用程序运行时内存中的技术手段。与传统的基于文件的WebShell不同，内存马不依赖于磁盘上的任何持久化存储，而是利用Java、.NET等语言提供的反射机制或动态编译功能，在内存中创建并执行恶意逻辑。这种方式不仅能够绕过大多数基于文件扫描的安全防护措施，而且由于其高度的灵活性和隐蔽性，使得一旦成功植入，很难被发现和清除。

接下来让我们先简单了解一下常见的Java内存马种类及如何使用。

## 内存马的类型

1. 动态注册 Servlet/Filter/Listener（使用 servlet-api 的具体实现）
2. 动态注册 Interceptor/Controller（使用框架如 Spring/Struts2）
3. 动态注册中间件实现（例如 Tomcat 的 Pipeline &amp; Valve）
4. 使用 Java Agent 技术写入字节码

## 注入方式

这里讨论作为攻击方常见的注入方式，修改目标Web应用源码或是直接新增一个后门文件这种方式不作讨论。

- 上传jsp文件，在jsp脚本中执行Java代码完成注入
- 上传agent.jar包，修改特定jvm进程的web应用字节码完成注入
- 存在jndi漏洞，通过jndi使目标服务器攻击方准备好的注入程序
- 存在反序列化漏洞，通过Java反序列化入口执行准备好的注入程序

## 本项目介绍

### 目录结构

| 项目名称                                 |介绍|
|--------------------------------------| ------|
| [servlet-api](servlet-api/README.md) |包含Servlet/Filter/Listener/Valve内存马，使用jsp注入方式|
| spring-shell-demo                    |包含Spring Controller/Interceptor内存马，使用内嵌代码注入|
| agent-inject                         |包含agent内存马，使用agentmain运行时动态注入|
| jndi                                 ||

