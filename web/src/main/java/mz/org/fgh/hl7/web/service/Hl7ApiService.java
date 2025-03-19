package mz.org.fgh.hl7.web.service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface Hl7ApiService {
    Map<String, Object> getProcessingStatus(Locale locale) throws InterruptedException, ExecutionException;
}
