package me.mywiki.configurator;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/**
 * <p>
 * A configuration is a map from name to values,
 * as such the contract for configuration is to map
 * what names are associated with each values
 * and provide a convenient mechanism for instantiating full maps
 * </p>
 * The contract between this library 
 * User defines 2 interfaces: the configuration interface is a read-only interface,
 * representing the completed (ready to consume) configuration
 *  example:
 *  
 *  <pre>
 *    interface MyConfiguration {
 *        String myProperty1()
 *        int  whateverName()
 *        MyValueClass myPropety3()
 *        // A configuration most likely needs to be composited hierarchically
 *        MySubConfiguration subConfiguration()
 *        // ... and so on
 *    }
 *   </pre>
 *  
 *    That's on one hand, the read-only interface defines the finished product (the sausage)
 *    
 *    In addition the client needs to declare how the sausage is made with a builder interface

 *    <pre>
 *    interface MyConfigurationBuilder {
 *      
 *      // All setters method will "return this" allowing setters chaining
 *      // A setter is any method that takes one parameter and return the bvuilder type 
 *      MyConfigurationBuilder myProperty1 ( String val_ );
 *      MyConfigurationBuilder whateverName ( int val_ );
 *      MyConfigurationBuilder myProperty3(  MyValueClass val_ );
 *      MyConfigurationBuilder subConfiguration ( MySubConfiguration sub_ );
 *      // ...
 *      
 *      // special methods
 *      // done finishes the building phase, and constructs the finished product
 *      // It'll throw IllegalStateException if not all properties have been set
 *      MyConfiguration done();
 *    }
 *    </pre>
 *    
 */
public class ReflectiveConfigurator {

    
        /**
         * DefaultsToString can be attached to any string read-property
         * specifying that in case the property is not explicitly set by the builder
         * it will be defaulted to the anotation value
         */
        @Retention(RetentionPolicy.RUNTIME) 
        @Target({ElementType.METHOD}) 
        public static @interface DefaultsToString {
            String val();
        }
        
        /**
         * Declare default value for int propety that it annotates
         */
        @Retention(RetentionPolicy.RUNTIME) 
        @Target({ElementType.METHOD}) 
        public static @interface DefaultsToInteger {
            int val();
        }
        
        /**
         * When the configuration value is a class
         * provides the default value in case the property is not set in the builder
         */
        @Retention(RetentionPolicy.RUNTIME) 
        @Target({ElementType.METHOD}) 
        public static @interface DefaultsToClass {
            Class<?> val();
        }
        
        /**
         * Apply a function to the supplied value
         */
        @Retention(RetentionPolicy.RUNTIME) 
        @Target({ElementType.METHOD}) 
        public static @interface TransformBy {
            Class<? extends Function<Object,Object>> _fun() default IdFun.class;
        }
        
     
        
        public static <Reader, Builder> 
            Builder configBuilderFor( Class<Reader>  readerClass, 
                                      Class<Builder> builderClass ) 
        {
            try {
                return new  ReflectiveBuilderImpl <Reader, Builder>( readerClass, builderClass)
                                        .makeBuilder();
            }
            catch (Exception ex) {
                if (ex instanceof RuntimeException) { throw (RuntimeException) ex; }
                else                                { throw new RuntimeException(ex); }
            }
        }
        
        public static class MissingPropertyException extends RuntimeException 
        {
            private static final long serialVersionUID = 1L;


            public MissingPropertyException(String message) {
                super(message);
             }

            public MissingPropertyException(Throwable cause) {
                super(cause);
             }
            
        }


        private static class ReflectiveBuilderImpl<Reader, Builder>
        {
     
            final Class<Builder> builderClass;
            final Class<Reader>  readerClass;
            
            //final Map<String,Object> valueMap= new HashMap<>();
            final Set<String> propNames;
            final Map<String, Function> transformers;
 
            ReflectiveBuilderImpl( Class<Reader> readerClass_, 
                                   Class<Builder> builderClass_) 
                                    throws Exception
            {
                this.builderClass= builderClass_;
                this.readerClass= readerClass_;
                Pair<Set<String>, Map<String,Function>> metadataCheck= checkAgainstSpec(readerClass, builderClass);
                
                this.propNames= metadataCheck.getLeft();
                this.transformers= metadataCheck.getRight();
            }
            

            /**
             * Checks that the builder interface matches the reader interface
             * @return a tuple containing all the property names, and a Map from 
             * propertyNames to the optional transforming function
             */
            private static 
                Pair<Set<String>, Map<String,Function>>
                    checkAgainstSpec( Class<?> readerClass_,
                                      Class<?> builderClass_) throws Exception
            {
                Preconditions.checkArgument(builderClass_.isInterface(), "builder should be an interface"); 
                
                Set<String> builderPropNames= new HashSet<>();
                
                for (Method m: builderClass_.getDeclaredMethods()) {
                    String mName= m.getName();
                    if (mName.equals("done")) {
                        Preconditions.checkArgument(  0 == m.getParameterCount(),"done is a method with 0 paramters");
                        Preconditions.checkArgument(m.getReturnType().equals(readerClass_), "done returns the reader object");
                        continue;
                    }
                    // all other methods are setter of form Builder propertyName(PropType val);
                    Preconditions.checkArgument(1 == m.getParameterCount(), "setter method: "+mName );
                    Preconditions.checkArgument(builderClass_.equals(m.getReturnType()), "returning a builder for"+mName );
                    builderPropNames.add(mName);
                }

                
                Set <String> readerPropNames=  new HashSet<String>();
                Map<String, Function> transformers= new HashMap<>();
                
                for (Method m: readerClass_.getMethods()) {
                    String mName= m.getName();
                    if (mName.equals("cloneBuilder")) {
                        Preconditions.checkArgument(  0 == m.getParameterCount(), "cloneBuilder is a method with 0 paramters" );
                        Preconditions.checkArgument( m.getReturnType().equals(builderClass_), "cloneBuilder returns the builder");
                        continue;
                    }
                    // all other methods are setter of form Builder propertyName(PropType val);
                    Preconditions.checkArgument( 0== m.getParameterCount() ,"getter method has 0 params "+mName );
                    readerPropNames.add(mName);
                    TransformBy transform= m.getDeclaredAnnotation(TransformBy.class);
                    if (transform != null) {
                       transformers.put(mName, transform._fun().newInstance()); 
                    }
                }
                
                Preconditions.checkArgument( readerPropNames.equals(builderPropNames), "Reader properties match builder properties");
                return new ImmutablePair<Set<String>, Map<String,Function>>(readerPropNames,transformers);
            }

            @SuppressWarnings("unchecked")
            public  Builder makeBuilder() 
            {
                return (Builder) 
                        Proxy.newProxyInstance(  this.getClass().getClassLoader(), 
                                                 new Class<?> [] {builderClass}, 
                                                 new ConfigBuilderHandler());
            }

            @SuppressWarnings("unchecked")
            public  Builder makeBuilder(Map<String,Object> initialValues) 
            {
                return (Builder) 
                        Proxy.newProxyInstance(  this.getClass().getClassLoader(), 
                                                 new Class<?> [] { builderClass }, 
                                                 new ConfigBuilderHandler(initialValues));
            }
     
            @SuppressWarnings("unchecked")
            public Reader buildTheReader(Map<String, Object> valueMap) 
            {
                // verify that we have values for all needed properties
                Set<String> suppliedKeys= valueMap.keySet();
                Set<String> toBeResolved=  new HashSet<String>(propNames);
                toBeResolved.removeAll(suppliedKeys);
                
                //check that all properties are assigned
                if (toBeResolved.isEmpty())
                    return (Reader)
                            Proxy.newProxyInstance(  this.getClass().getClassLoader(), 
                                                     new Class<?> [] { readerClass , InternalReaderAccess.class}, 
                                                     new ConfigReaderHandler( valueMap) );
                else {
                    //TODO: supply a list of what is missing
                    throw new MissingPropertyException("Configuration missing the following properties: " + toBeResolved.toString());
                }
                    
            }
            
            private class ConfigBuilderHandler implements InvocationHandler {
                final Map<String,Object> valueMap;
                
                public ConfigBuilderHandler() {
                    this.valueMap= new HashMap<>();
                }
                
                public ConfigBuilderHandler(Map<String, Object> initialValues) {
                    this.valueMap= new HashMap<>(initialValues);
                }

                @Override
                public Object invoke(Object proxy, Method method, Object[] args)
                        throws Throwable {
                    String mName= method.getName();
                    if (mName.equals("done")) {
                        return buildTheReader(this.valueMap);
                    }
                    // here we assume that all the other methods have the shape
                    // XXXBuilder propertyName( PropertyType val_)
                    // because the builder constructor enforces this condition
                    if (args.length != 1 ) {
                        throw new IllegalStateException("Expecting propety setter, of type XXXBuilder propertyName( PropertyType val_)");
                    }
                    Object val= args[0];
                    if (transformers.containsKey(mName)) {
                        val= transformers.get(mName).apply(val);
                    }
                    valueMap.put(mName,val);
                    return proxy;
                }
            }
            
            public class ConfigReaderHandler implements InvocationHandler {
                
                final Map<String,Object>myValueMap;

                public ConfigReaderHandler(Map<String, Object> valueMap) {
                    // copy the input to avoid side effects
                    this.myValueMap= new HashMap<String, Object>(valueMap);
                }

                @Override
                public Object invoke ( Object proxy, 
                                       Method m, 
                                       Object[] args)
                        throws Throwable 
                {
                    String mName= m.getName();
                    
                    switch (mName) {
                    // Begin special cases of ReadOnly interface
                        case "cloneBuilder":  { return makeBuilder(this.myValueMap); }
                    
                        case "toString": { Verify.verify(args == null); 
                                           return myValueMap.toString(); }
                        case "__internalMap":  { Verify.verify(args == null);
                                                 return this.myValueMap; }
                    
                        case "equals" : { Verify.verify(args.length == 1);
                                          if (args[0] == null) return false;
                                          if (! (args[0] instanceof InternalReaderAccess ))
                                              return false;
                                          return (this.myValueMap.equals(((InternalReaderAccess) args[0]).__internalMap()));
                                        }
                        case "hashCode" : { Verify.verify(args == null);
                                            return myValueMap.hashCode();
                                           }
                    } //End special cases
                    
                    
                    // all the rest are to be considered accessor methods
                    if (myValueMap.containsKey(mName)) {
                        return myValueMap.get(mName);
                    }
                    else {
                        throw new IllegalStateException("Value not supplied for property: "+mName);
                    }
                        
                }

            }
            
            /**
             * to be used by us in helper methods to dynamically access the internal state of our implementation
             */
            private static interface InternalReaderAccess {
                Map<String,Object> __internalMap();
            }
        }// end of ReflectiveBuilderImple
        
        private static class IdFun implements Function<Object, Object> { @Override public Object apply(Object x) { return x; }}
 
}
