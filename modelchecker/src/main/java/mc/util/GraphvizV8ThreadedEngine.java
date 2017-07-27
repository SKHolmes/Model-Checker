package mc.util;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8RuntimeException;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import guru.nidi.graphviz.engine.AbstractJsGraphvizEngine;
import guru.nidi.graphviz.engine.GraphvizException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GraphvizV8ThreadedEngine extends AbstractJsGraphvizEngine {
    private static final Pattern ABORT = Pattern.compile("^undefined:\\d+: abort");
    private static final Pattern ERROR = Pattern.compile("^undefined:\\d+: (.*?)\n");
    private ThreadLocal<V8Array> messages = new ThreadLocal<>();
    private ThreadLocal<V8> v8 = new ThreadLocal<>();

    public GraphvizV8ThreadedEngine() {
        super(true);
    }

    @Override
    public void release() {
    }
    @Override
    protected void doInit() throws Exception {
        v8.set(V8.createV8Runtime());
        v8.get().executeVoidScript(jsInitEnv());
        messages.set(v8.get().getArray("$$prints"));
        v8.get().executeVoidScript(jsVizCode("1.8.0"));
    }

    @Override
    protected String jsExecute(String call) {
        if (v8.get() == null) {
            try {
                doInit();
            } catch (Exception e) {
                throw new GraphvizException("Problem executing graphviz", e);
            }
        }
        try {
            return v8.get().executeStringScript(call);
        } catch (V8RuntimeException e) {
            if (ABORT.matcher(e.getMessage()).find()) {
                throw new GraphvizException(IntStream.range(0, messages.get().length())
                    .mapToObj(i -> V8ObjectUtils.getValue(messages.get(), i).toString())
                    .collect(Collectors.joining("\n")));
            }
            final Matcher em = ERROR.matcher(e.getMessage());
            if (em.find()) {
                throw new GraphvizException(em.group(1));
            }
            throw new GraphvizException("Problem executing graphviz", e);
        }
    }
}