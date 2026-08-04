package com.kazuto.talkon.common;
import java.util.Map;
public record ApiError(String code,String message,Map<String,String> fieldErrors,String traceId){}

