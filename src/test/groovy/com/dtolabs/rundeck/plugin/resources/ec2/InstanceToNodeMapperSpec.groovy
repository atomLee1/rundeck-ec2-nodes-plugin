package com.dtolabs.rundeck.plugin.resources.ec2

import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.InstanceState
import com.amazonaws.services.ec2.model.InstanceStateName
import com.amazonaws.services.ec2.model.Tag
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 12/16/16
 */
class InstanceToNodeMapperSpec extends Specification {
    def "single selector valid properties"() {
        given:
        def i = mkInstance()

        when:
        def result = InstanceToNodeMapper.applySingleSelector(i, selector)

        then:
        result == expect

        where:
        selector           | expect
        'instanceId'       | 'aninstanceId'
        'architecture'     | 'anarch'
        'state.name'       | 'running'
        'privateIpAddress' | '127.0.9.9'
        'publicDnsName'    | null
    }

    @Unroll
    def "single selector invalid properties #selector"() {
        given:
        def i = mkInstance()

        when:
        def result = InstanceToNodeMapper.applySingleSelector(i, selector)

        then:
        result == null
        InstanceToNodeMapper.GeneratorException e = thrown()
        e != null

        where:
        selector       | _
        'fromtom'      | _
        'bigglesworth' | _
        'badapple'     | _
    }

    def "single selector tags"() {
        given:
        def i = mkInstance()

        when:
        def result = InstanceToNodeMapper.applySingleSelector(i, selector)

        then:
        result == expect

        where:
        selector       | expect
        'tags/Name'    | 'bob'
        'tags/env'     | 'PROD'
        'tags/missing' | null
    }

    @Unroll
    def "apply selector #selector"() {
        given:
        def i = mkInstance()
        when:
        def result = InstanceToNodeMapper.applySelector(i, selector, defVal)
        then:
        result == expect

        where:
        selector                       | defVal      | expect
        'instanceId,publicDnsName'     | null        | 'aninstanceId'
        'publicDnsName,instanceId'     | null        | 'aninstanceId'
        'privateIpAddress,instanceId'  | null        | '127.0.9.9'
        'instanceId,privateIpAddress'  | null        | 'aninstanceId'
        'publicDnsName,privateDnsName' | null        | null
        'publicDnsName,privateDnsName' | 'a default' | 'a default'
    }

    def "apply selector multipart"() {
        given:
        def i = mkInstance()
        when:
        def result = InstanceToNodeMapper.applySelector(i, selector, defVal)
        then:
        result == expect

        where:
        selector                            | defVal      | expect
        'instanceId+publicDnsName'          | null        | 'aninstanceId'
        'instanceId+\'_\'+publicDnsName'    | null        | 'aninstanceId_'
        'instanceId+"_"+publicDnsName'      | null        | 'aninstanceId_'
        'publicDnsName+instanceId'          | null        | 'aninstanceId'
        'publicDnsName+\'_\'+instanceId'    | null        | '_aninstanceId'
        'publicDnsName+"/"+instanceId'      | null        | '/aninstanceId'
        'privateIpAddress+instanceId'       | null        | '127.0.9.9aninstanceId'
        'privateIpAddress+\'_\'+instanceId' | null        | '127.0.9.9_aninstanceId'
        'privateIpAddress+"::"+instanceId'  | null        | '127.0.9.9::aninstanceId'
        'instanceId+privateIpAddress'       | null        | 'aninstanceId127.0.9.9'
        'instanceId+"-"+privateIpAddress'   | null        | 'aninstanceId-127.0.9.9'
        'publicDnsName+instanceId'          | null        | 'aninstanceId'
        'publicDnsName+"-"+instanceId'      | null        | '-aninstanceId'
        'publicDnsName+privateDnsName'      | null        | null
        'publicDnsName+"_"+privateDnsName'  | null        | null
        'publicDnsName+privateDnsName'      | 'a default' | 'a default'
        'publicDnsName+"_"+privateDnsName'  | 'a default' | 'a default'
        'tags/Name+"-"+instanceId'          | null        | 'bob-aninstanceId'
    }

    def "apply selector merged tags"() {
        given:
        def i = mkInstance()
        when:
        def result = InstanceToNodeMapper.applySelector(i, selector, defVal, true)
        then:
        result == expect

        where:
        selector                       | defVal      | expect
        'tags/Name|tags/env'           | null        | 'bob,PROD'
        'publicDnsName|instanceId'     | null        | 'aninstanceId'
        'privateIpAddress|instanceId'  | null        | '127.0.9.9,aninstanceId'
        'instanceId|privateIpAddress'  | null        | 'aninstanceId,127.0.9.9'
        'publicDnsName|instanceId'     | null        | 'aninstanceId'
        'publicDnsName|privateDnsName' | null        | null
        'publicDnsName|privateDnsName' | 'a default' | 'a default'
    }

    def "apply selector merged tags multi"() {
        given:
        def i = mkInstance()
        when:
        def result = InstanceToNodeMapper.applySelector(i, selector, defVal, true)
        then:
        result == expect

        where:
        selector                            | defVal | expect
        'instanceId|tags/Name+"_"+tags/env' | null   | 'aninstanceId,bob_PROD'
    }

    private static Instance mkInstance() {
        Instance i = new Instance()
        i.withTags(new Tag('Name', 'bob'), new Tag('env', 'PROD'))
        i.setInstanceId("aninstanceId")
        i.setArchitecture("anarch")

        def state = new InstanceState()
        state.setName(InstanceStateName.Running)
        i.setState(state)
        i.setPrivateIpAddress('127.0.9.9')
        return i
    }
}
