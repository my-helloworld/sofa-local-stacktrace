package com.alipay.sofa;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.runtime.api.annotation.SofaReference;
import com.alipay.sofa.runtime.api.annotation.SofaReferenceBinding;
import com.alipay.sofa.runtime.api.annotation.SofaService;
import com.alipay.sofa.runtime.api.annotation.SofaServiceBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.stream.Stream;

/**
 * 服务提供者
 *
 * @author chpengzh.
 * @since 2019/1/20
 */
@SpringBootApplication
public class Application {

    @Component("attach-stacktrace")
    public static class StacktraceFilter extends Filter {

        @Override
        public SofaResponse invoke(
            FilterInvoker filterInvoker,
            SofaRequest sofaRequest
        ) throws SofaRpcException {
            SofaResponse res = filterInvoker.invoke(sofaRequest);
            if (res.getAppResponse() instanceof Throwable) {
                StackTraceElement[] current = Thread.currentThread().getStackTrace();
                Throwable err = (Throwable) res.getAppResponse();
                err.setStackTrace(Stream.concat(
                    Stream.of(err.getStackTrace()),
                    Stream.of(current)
                ).toArray(StackTraceElement[]::new));
            }
            return res;
        }
    }

    @Controller
    public static class DemoController {

        @SofaReference(
            jvmFirst = false,
            binding = @SofaReferenceBinding(bindingType = "bolt", filters = "attach-stacktrace")
        )
        private Application.DemoAPI api;

        @GetMapping
        public String doTest() {
            api.doBreak();
            return "hello";
        }

    }

    public interface DemoAPI {

        void doBreak();

    }

    @Service
    @SofaService(bindings = @SofaServiceBinding(bindingType = "bolt"))
    public static class DemoImpl implements DemoAPI {

        @Override
        public void doBreak() {
            throw new RuntimeException("(^_^)");
        }

    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
