package org.xillium.core.management;

import java.lang.reflect.Field;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.xillium.core.*;
import org.xillium.core.conf.ServiceAugmentation;
import org.xillium.core.util.*;


/**
 * SystemCommander retrieves OS bean information into a DataBinder.
 * <ul>
 * <li>a(async)     asynchronous processing</li>
 * <li>f(forward)   forward</li>
 * <li>x(exec)      execute</li>
 * </ul>
 */
public class SystemCommander {
    private static final String MESSAGE = "jos.exception";
    private static final String OUTPUT  = "jos.output";

    private static List<ServiceAugmentation.Spec> _augmentations = new ArrayList<ServiceAugmentation.Spec>();
    private static AtomicInteger _sequence = new AtomicInteger();

    private final DataBinder _binder;
    private final Map<String, Service> _services;
    private boolean _async;
    private boolean _verbose;

    public SystemCommander(DataBinder binder,  Map<String, Service> services) {
        _binder = binder;
        _services = services;
    }

    public SystemCommander v(boolean verbose) {
        _verbose = verbose;
        return this;
    }

    public SystemCommander a(boolean async) {
        _async = async;
        return this;
    }

    public SystemCommander t() {
        StringBuilder sb = new StringBuilder("\n");
        for (int i = 0; i < _augmentations.size(); ++i) {
            sb.append('(').append(i).append(')').append(_augmentations.get(i).toString()).append('\n');
        }
        _binder.put("augmentations", sb.toString());
        return this;
    }

    public SystemCommander f(String address, String script) {
        try {
            ScriptableServiceFilter filter = new ScriptableServiceFilter();
            filter.setAcknowledge(script);
            ((Service.Extendable)_services.get(address)).setFilter(filter);
            synchronized (_augmentations) { _augmentations.add(new ServiceAugmentation.Spec(address, filter)); }
        } catch (Exception x) {
            _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public SystemCommander m(String address, String name, String script) {
        try {
            ScriptableMilestoneEvaluation eval = new ScriptableMilestoneEvaluation(script);
            ServiceMilestone.attach(_services.get(address), name, eval);
            synchronized (_augmentations) { _augmentations.add(new ServiceAugmentation.Spec(address, name, eval)); }
        } catch (Exception x) {
            _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public SystemCommander u(String index) {
        try {
            ServiceAugmentation.Spec spec = _augmentations.get(Integer.parseInt(index));
            if (spec.milestone != null) {
                ServiceMilestone.detach(_services.get(spec.service), spec.milestone, (ServiceMilestone.Evaluation)spec.augment);
            } else {
                Service service = _services.get(spec.service);
                Field field = Beans.getKnownField(service.getClass(), "_filter");
                field.set(service, CompoundServiceFilter.cleanse(field.get(service), (Service.Filter)spec.augment));
            }
            synchronized (_augmentations) { _augmentations.remove(spec); }
        } catch (Exception x) {
            _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public SystemCommander x(String line) {
        try {
            String[] args = line.split(" +");
            Process process = new ProcessBuilder(Arrays.asList(args)).redirectErrorStream(true).start();
            Object sequence = "#" + _sequence.incrementAndGet();
            try {
                Field field = process.getClass().getDeclaredField("pid");
                field.setAccessible(true);
                sequence = field.get(process);
            } catch (Exception x) {
            }
            if (!_async) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                try {
                    StringBuilder output = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append('\n');
                    }
                    if (output.length() > 0) output.setLength(output.length() - 1);
                    _binder.put(OUTPUT + '[' + args[0] + '(' + sequence + ")]", output.toString());
                } finally {
                    reader.close();
                }
            } else {
                process.getInputStream().close();
            }
        } catch (Exception x) {
            _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }
}
