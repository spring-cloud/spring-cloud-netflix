package contracts

org.springframework.cloud.contract.spec.Contract.make {
  // Human readable description
  description 'Should produce valid metrics data'
  // Label by means of which the output message can be triggered
  label 'metrics'
  // input to the contract
  input {
    // the contract will be triggered by a method
    triggeredBy('createMetricsData()')
  }
  // output message of the contract
    outputMessage {
        // destination to which the output message will be sent
        sentTo 'hystrixStreamOutput'
        headers {
            header('contentType': 'application/json')
        }
        body("""
{
   "origin":{
      "host":"192.168.1.192",
      "port":0,
      "serviceId":"application",
      "id":"application:0"
   },
   "data":{
      "type":"HystrixCommand",
      "name":"application.hello",
      "group":"Application",
      "currentTime":1494840901153,
      "isCircuitBreakerOpen":false,
      "errorPercentage":0,
      "errorCount":0,
      "requestCount":1,
      "rollingCountCollapsedRequests":0,
      "rollingCountExceptionsThrown":0,
      "rollingCountFailure":0,
      "rollingCountFallbackFailure":0,
      "rollingCountFallbackRejection":0,
      "rollingCountFallbackSuccess":0,
      "rollingCountResponsesFromCache":0,
      "rollingCountSemaphoreRejected":0,
      "rollingCountShortCircuited":0,
      "rollingCountSuccess":0,
      "rollingCountThreadPoolRejected":0,
      "rollingCountTimeout":0,
      "currentConcurrentExecutionCount":0,
      "latencyExecute_mean":0,
      "latencyExecute":{
         "0":0,
         "25":0,
         "50":0,
         "75":0,
         "90":0,
         "95":0,
         "99":0,
         "99.5":0,
         "100":0
      },
      "latencyTotal_mean":0,
      "latencyTotal":{
         "0":0,
         "25":0,
         "50":0,
         "75":0,
         "90":0,
         "95":0,
         "99":0,
         "99.5":0,
         "100":0
      },
      "propertyValue_circuitBreakerRequestVolumeThreshold":20,
      "propertyValue_circuitBreakerSleepWindowInMilliseconds":5000,
      "propertyValue_circuitBreakerErrorThresholdPercentage":50,
      "propertyValue_circuitBreakerForceOpen":false,
      "propertyValue_circuitBreakerForceClosed":false,
      "propertyValue_circuitBreakerEnabled":true,
      "propertyValue_executionIsolationStrategy":"THREAD",
      "propertyValue_executionIsolationThreadTimeoutInMilliseconds":1000,
      "propertyValue_executionIsolationThreadInterruptOnTimeout":true,
      "propertyValue_executionIsolationThreadPoolKeyOverride":null,
      "propertyValue_executionIsolationSemaphoreMaxConcurrentRequests":10,
      "propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests":10,
      "propertyValue_metricsRollingStatisticalWindowInMilliseconds":10000,
      "propertyValue_requestCacheEnabled":true,
      "propertyValue_requestLogEnabled":true,
      "reportingHosts":1
   }
}
""")
        testMatchers {
           jsonPath('$.origin', byCommand('assertOrigin($it)'))
           jsonPath('$.data', byCommand('assertData($it)'))
           jsonPath('$.data.errorCount', byEquality())
           jsonPath('$.data.errorPercentage', byEquality())
           jsonPath('$.data.requestCount', byType())
           jsonPath('$.data.currentConcurrentExecutionCount', byType())
           jsonPath('$.data.rollingCountFailure', byEquality())
           jsonPath('$.data.rollingCountSuccess', byType())
           jsonPath('$.data.rollingCountShortCircuited', byEquality())
           jsonPath('$.data.rollingCountFallbackSuccess', byEquality())
           jsonPath('$.data.isCircuitBreakerOpen', byEquality())
        }
    }
}
