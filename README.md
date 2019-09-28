# Simple configuration for Java

This modest package tries to solve a very simple but very persistent and annoying problem : ***suplying configuration values*** , such as Urls, database passwords and the likes.

The astute reader will surely ask if the author of the package has gone mad and decided to reinvent the wheel that other people have solved with everything from old .properties file to XML, Json, Yaml in conjunction with countless and varied frameworks (Spring and Guava being the most popular to claim that they solve this particular problem and many more), dependency injection and the likes. 

This will be explained in due course, the reason this modest package is in existence is because the author has seen those and many other frameworks creating more and more serious problems than they solved.

# The contract at first glance.

A configuration is a set of name value pairs, and because we are in Java world we like to thin as a set of ***typed*** name value pairs. In Java we model this with an interface, like in this example :


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
    
    // imagine we'll be using some connection pooling library
    int maxOpenConnections();
    
    // optionally, the Config interface may contai
    // a special method with the following signature, to be explained later
    MyDbConfiBuilder cloneBuilder()
  }
```

Now if somebody supplies a MyDbConfig object to us, that's all we need to know to connect to a database. So if we have a class MyDbInteractions , all we need to do is ask for that in the constructor


```java
  class MyDbInteractions {
    private final MyDbConfig dbConfig;
    MyDbInteractions( MyDbConfig dbConfig_ ) { this.dbConfig = dbConfig_; } // and we're in business
    // ... 
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
     MyDbConfigBuilder maxOpenConnections (int max);
     
     MyDbConfig done();
   }
```

Now, the developer having ***declared*** this ***pair of matching interfaces*** is pretty much done in the sense that the rather boring task of implementing these interfaces is left for the framework. So let's construct a set of configurations:

```java
     class ValidConfigurations {
        
        public static PROD_DB_CONFIG = ReflectiveConfigurator.configBuilderFor( MyDbConfig.class, MyDbConfigBuilder.class)
                                          .jdbcUrl("jdbc:oracle:thin:@//myProdDbServer:1521/orcl")
                                          .username("blah")
                                          .password("blah")
                                          .maxConnections(20)
                                          .done();
        public static DEV_DB_CONFIG = ReflectiveConfigurator.configBuilderFor( MyDbConfig.class, MyDbConfigBuilder.class)
                                          .jdbcUrl("jdbc:oracle:thin:@//localhost:1521/orcl")
                                          .username("devblah")
                                          .password("devblah")
                                          .maxConnections(5)
                                          .done();                                          

        public static PROD_STANDBY_DBCONFIG = 
                                        PROD_DB_CONFIG.cloneBuilder()
                                          .jdbcUrl("jdbc:oracle:thin:@//standbyDBserver:1521/orcl")
                                          .done();                                          

    }
```    
 
Note in the code above, the usage of cloneBuilder() which is utilize to "clone" configuration object in order to change only a few parameters.
 
*** That's all folks ! *** 

Well not quite, this package has a few extra goodies (like default values, transform functions , such that we do not have to store passwords in clear text as in the contrived example above, to be detailed as the time allows. Plus, there's so,mething to be said about rationale.

## Rationale

The argument elaboration is in the making ...
