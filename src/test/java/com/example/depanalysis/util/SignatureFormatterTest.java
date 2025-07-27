package com.example.depanalysis.util;

public class SignatureFormatterTest {
    public static void main(String[] args) {
        // Test cases
        System.out.println("Testing SignatureFormatter:");
        
        String result1 = SignatureFormatter.formatMethodSignature("trace", "(Ljava/lang/Object;)V");
        System.out.println("trace(Ljava/lang/Object;)V -> " + result1);
        
        String result2 = SignatureFormatter.formatMethodSignature("parseBase64Binary", "(Ljava/lang/String;)[B");
        System.out.println("parseBase64Binary(Ljava/lang/String;)[B -> " + result2);
        
        String result3 = SignatureFormatter.formatMethodSignature("execute", "(Lorg/apache/http/client/methods/HttpUriRequest;)Lorg/apache/http/HttpResponse;");
        System.out.println("execute(Lorg/apache/http/client/methods/HttpUriRequest;)Lorg/apache/http/HttpResponse; -> " + result3);
        
        String result4 = SignatureFormatter.formatClassName("org/apache/http/client/HttpClient");
        System.out.println("org/apache/http/client/HttpClient -> " + result4);
    }
}
