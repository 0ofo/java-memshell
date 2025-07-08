# Listener内存马

Listener 它是 JavaEE 的规范，就是接口。用于监听 JavaWeb 程序中的事件。（如：ServletContext、HttpSession、ServletRequest的创建、修改和删除）

监听器的作用是，监听某种事物的变化。然后通过回调函数，反馈给客户（程序）去做一些相应的处理。

在 JavaWeb 中，可以为某些事件来设置监听器，当事件被触发时，监听器中的指定方法会被调用。

Servlet程序、Filter 过滤器 和 Listener 监听器 并称 WEB 的三大组件。

Listener 的原理是基于观察者模式的，所谓的观察者模式简单来说，就是当被观察者的特定事件被触发（一般在某些方法被调用后），会通知观察者（调用观察者的方法），观察者可以在自己的的方法中来对事件做一些处理。

## Java Web中常见的Listener类型

|类型|触发事件|
| ------------------------| ----------------------------------------------------------------------------------------|
|ServletContextListener|在ServletContext创建和关闭时都会通知ServletContextListener监听器。|
|HttpSessionListener|当一个HttpSession刚被创建或者失效（invalidate）的时候，将会通知HttpSessionListener监听|
|ServletRequestListener|在ServletRequest创建和关闭时都会通知ServletRequestListener监听器|

## ServletRequestListener的生命周期

![image](assets/image-20250619095415-queqcu7.png)
