package org.springframework.cloud.netflix.feign.support;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import feign.MethodMetadata;

/**
 * @author chadjaros
 */
public class SpringMvcContractTest {

    private SpringMvcContract contract;

    @Before
    public void setup() {
        contract = new SpringMvcContract();
    }

    @Test
    public void testProcessAnnotationOnMethod_Simple() throws Exception {
        Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest", String.class);
        MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

        assertEquals("/test/{id}", data.template().url());
        assertEquals("GET", data.template().method());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, data.template().headers().get("Accept").iterator().next());
    }

    @Test
    public void testProcessAnnotations_Simple() throws Exception {
        Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest", String.class);
        MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

        assertEquals("/test/{id}", data.template().url());
        assertEquals("GET", data.template().method());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, data.template().headers().get("Accept").iterator().next());

        assertEquals("id", data.indexToName().get(0).iterator().next());
    }

    @Test
    public void testProcessAnnotationsOnMethod_Advanced() throws Exception {
        Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest", String.class, String.class, Integer.class);
        MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

        assertEquals("/advanced/test/{id}", data.template().url());
        assertEquals("PUT", data.template().method());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, data.template().headers().get("Accept").iterator().next());
    }

    @Test
    public void testProcessAnnotationsOnMethod_Advanced_UnknownAnnotation() throws Exception {
        Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest", String.class, String.class, Integer.class);
        contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

        // Don't throw an exception and this passes
    }

    @Test
    public void testProcessAnnotations_Advanced() throws Exception {
        Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest", String.class, String.class, Integer.class);
        MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

        assertEquals("/advanced/test/{id}", data.template().url());
        assertEquals("PUT", data.template().method());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, data.template().headers().get("Accept").iterator().next());

        assertEquals("Authorization", data.indexToName().get(0).iterator().next());
        assertEquals("id", data.indexToName().get(1).iterator().next());
        assertEquals("amount", data.indexToName().get(2).iterator().next());

        assertEquals("{Authorization}", data.template().headers().get("Authorization").iterator().next());
        assertEquals("{amount}", data.template().queries().get("amount").iterator().next());
    }

    @Test
    public void testProcessAnnotations_Advanced2() throws Exception {
        Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest");
        MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

        assertEquals("/advanced", data.template().url());
        assertEquals("GET", data.template().method());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, data.template().headers().get("Accept").iterator().next());
    }

    @Test
    public void testProcessAnnotations_Advanced3() throws Exception {
        Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest");
        MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

        assertEquals("", data.template().url());
        assertEquals("GET", data.template().method());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, data.template().headers().get("Accept").iterator().next());
    }

    public interface TestTemplate_Simple {
        @RequestMapping(value = "/test/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        ResponseEntity<TestObject> getTest(@PathVariable("id") String id);

        @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        TestObject getTest();
    }

    @JsonAutoDetect
    @RequestMapping("/advanced")
    public interface TestTemplate_Advanced {

        @ExceptionHandler
        @RequestMapping(value = "/test/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
        ResponseEntity<TestObject> getTest(@RequestHeader("Authorization") String auth, @PathVariable("id") String id, @RequestParam("amount") Integer amount );

        @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        TestObject getTest();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
    public class TestObject {

        public String something;
        public Double number;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestObject that = (TestObject) o;

            if (number != null ? !number.equals(that.number) : that.number != null) return false;
            if (something != null ? !something.equals(that.something) : that.something != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (something != null ? something.hashCode() : 0);
            result = 31 * result + (number != null ? number.hashCode() : 0);
            return result;
        }
    }
}