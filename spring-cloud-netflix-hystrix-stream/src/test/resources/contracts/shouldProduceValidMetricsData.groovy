package contracts

import org.springframework.cloud.netflix.hystrix.contract.HystrixContractUtils

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
        body(HystrixContractUtils.simpleBody())
        testMatchers {
           jsonPath('$.origin', byCommand('assertOrigin($it)'))
           jsonPath('$.event', byCommand('assertEvent($it)'))
           jsonPath('$.data', byCommand('assertData($it)'))
        }
    }
}
