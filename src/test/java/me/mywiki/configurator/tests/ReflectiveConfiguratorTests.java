package me.mywiki.configurator.tests;


import static org.junit.Assert.*;

import java.util.function.Function;

import static org.hamcrest.core.Is.*;
import org.junit.Test;
import org.junit.experimental.categories.Categories.ExcludeCategory;

import me.mywiki.configurator.ReflectiveConfigurator;
import me.mywiki.configurator.ReflectiveConfigurator.TransformBy;
import me.mywiki.configurator.ReflectiveConfigurator.MissingPropertyException;

public class ReflectiveConfiguratorTests {

    /**
     * basic configuration
     */
    public static interface Configuration1 {
        
        public String property1();
        
        public int property2();
        
        public Builder cloneBuilder();
        
        public static interface Builder {
            public Builder property1(String v);
            public Builder property2(int v);
            public Configuration1 done();
        }
    }
    
    /**
     * basic test:
     * - we can assign value and read them back,
     * - hashCode() and equals() are implemented correctly
     */
    @Test
    public void testCase1() {
        Configuration1 config11= ReflectiveConfigurator.configBuilderFor( Configuration1.class, Configuration1.Builder.class)
                                .property1("value1")
                                .property2(5)
                                .done();
        assertThat(config11.property1(), is("value1"));
        assertThat(config11.property2(), is(5));
        
        Configuration1 config12= config11.cloneBuilder()
                                         .property2(3)
                                         .done();
        
        assertThat(config12.property1(), is("value1"));
        assertThat(config12.property2(), is(3));
        
        // also make sure that in the process of cloning we don't side effect the original
        assertThat(config11.property2(), is(5));
        
        //let's create another object, identicat with 11, check that hashCode and equas
        Configuration1 config13= config12.cloneBuilder().property2(5).done();
        assertThat(config11, is(config13));
        assertThat(config11.hashCode(), is( config13.hashCode()));
        //
    }
    
    /**
     * Test that we throw on missing values
     */
    @Test( expected = MissingPropertyException.class )
    public void testMissingPropertiesAreThrown() {
        Configuration1 config11= ReflectiveConfigurator.configBuilderFor( Configuration1.class, Configuration1.Builder.class)
                .property1("value1")
                .done();   
    }

    /**
     * Test that a value set in the builder (at compile time)
     * is transformed by a function, at runtime
     */
    public static interface Configuration2 {
        int intVal1();
        String strVal2();
        
        /**
         * this tests that the config framework applies a transformation
         */
        @TransformBy(_fun = TestTransformer.class)
        String transformedVal();
        
        static class TestTransformer implements Function<Object, Object> { @Override public Object apply(Object t) {
                return t+"." + t;
        }}
        
        public static interface Builder {
            Builder intVal1(int val_);
            Builder strVal2(String val_);
            
            Builder transformedVal( String val_);
            Configuration2 done();
        }
    }
    
    @Test
    public void testCase2() {
        Configuration2 config2= ReflectiveConfigurator.configBuilderFor( Configuration2.class, Configuration2.Builder.class)
                                .intVal1(1)
                                .strVal2("val2")
                                .transformedVal("val2")
                                .done();
        assertThat(config2.intVal1(), is(1));
        assertThat(config2.strVal2(), is("val2"));
        assertThat(config2.transformedVal(), is("val2.val2"));
        
    }

}
