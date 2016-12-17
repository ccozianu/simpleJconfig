# Simple configuration for Java

This modest package tries to solve a very simple but very persistent and annoying problem : supplying configuration values , such as Urls, database passwords
and the likes.

The astute reader will surely ask if the author of the package has gone mad and decided to reinvent the wheel that other people have solved with .properties file or with countless and varied frameworks (Spring and Guava being the most popular to claim that they solve this particular problem and many more). 

This will be explained in due course, the reason this modest package is in existence is because the author has seen those and many other frameworks creating more and more serious problems than they solved.

# The contract at first glance.

A configuration is a set of name value pairs, and because we are in Java world we like to thin as a set of ***typed*** name value pairs. In Java we model this with an interface:


```java
  interface MyDbConfig {
    String jdbcUrl();
    String username();
    String password();
    /**
     * JDBC api allows customization of connection setting through java.util.Properties to be passed to the driver
     * when requesting the connextion
     */
    Properties extraProperties();
  }
```

Now if somebody supplies a MyDbConfig object to us, that's all we need to know to connect to a database. So if we have a class MyDbInteractions , all we need to do is ask for that in the constructor


```java
  class MyDbInteractions {
    MyDbInteractions( MyDbConfig dbConfig_ ) { this.dbConfig = dbConfig_; } // and we're in business
  }
```
But who's gonna give us those values, somebody needs to gather those values and put them together, and since typical applications have a lot more than 3 config values, we tend to need to use a builder patternt to construct the object that implements the config interface. 

So what is the builder in this case ? Let's declare it as an interface we expect to use to be able to conveniently gather all the values.

```java
   interface MyDbConfigBuilder {
     MyDbConfigBuilder jdbcUrl(String val_);
     MyDbConfigBuilder username(String val_);
     MyDbConfigBuilder password(String val_);
     MyDbConfigBuilder extraProperties(Properties props);
     MyDbConfig done();
   }
```
