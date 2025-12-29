/**
 * Demo process templates for the process designer
 * Users can select from these templates when creating a new process
 */

export interface DemoProcess {
  id: string;
  name: string;
  description: string;
  category: string;
  bpmn: string;
}

export const demoProcesses: DemoProcess[] = [
  {
    id: 'blank',
    name: 'Blank Process',
    description: 'Start with an empty process canvas',
    category: 'Basic',
    bpmn: `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="process_1" name="New Process" isExecutable="true">
    <bpmn:startEvent id="startEvent" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:endEvent id="endEvent" name="End">
      <bpmn:incoming>Flow_1</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="startEvent" targetRef="endEvent"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="process_1">
      <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
        <dc:Bounds x="152" y="102" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="158" y="145" width="24" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent">
        <dc:Bounds x="392" y="102" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="400" y="145" width="20" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="188" y="120"/>
        <di:waypoint x="392" y="120"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
  },
  {
    id: 'simple-approval',
    name: 'Simple Approval',
    description: 'Basic approval workflow with one approver',
    category: 'Approval',
    bpmn: `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="simple-approval" name="Simple Approval" isExecutable="true">
    <bpmn:documentation>Simple approval workflow with approve/reject decision</bpmn:documentation>
    <bpmn:startEvent id="startEvent" name="Request Submitted">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="approvalTask" name="Review Request" flowable:candidateGroups="supervisors">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:exclusiveGateway id="decision" name="Decision">
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_approved</bpmn:outgoing>
      <bpmn:outgoing>Flow_rejected</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:endEvent id="endApproved" name="Approved">
      <bpmn:incoming>Flow_approved</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:endEvent id="endRejected" name="Rejected">
      <bpmn:incoming>Flow_rejected</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="startEvent" targetRef="approvalTask"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="approvalTask" targetRef="decision"/>
    <bpmn:sequenceFlow id="Flow_approved" sourceRef="decision" targetRef="endApproved">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${decision == 'approved'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_rejected" sourceRef="decision" targetRef="endRejected">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${decision == 'rejected'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="simple-approval">
      <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
        <dc:Bounds x="100" y="182" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="approvalTask_di" bpmnElement="approvalTask">
        <dc:Bounds x="200" y="160" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="decision_di" bpmnElement="decision" isMarkerVisible="true">
        <dc:Bounds x="365" y="175" width="50" height="50"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="endApproved_di" bpmnElement="endApproved">
        <dc:Bounds x="500" y="102" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="endRejected_di" bpmnElement="endRejected">
        <dc:Bounds x="500" y="252" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="136" y="200"/>
        <di:waypoint x="200" y="200"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="300" y="200"/>
        <di:waypoint x="365" y="200"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_approved_di" bpmnElement="Flow_approved">
        <di:waypoint x="390" y="175"/>
        <di:waypoint x="390" y="120"/>
        <di:waypoint x="500" y="120"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_rejected_di" bpmnElement="Flow_rejected">
        <di:waypoint x="390" y="225"/>
        <di:waypoint x="390" y="270"/>
        <di:waypoint x="500" y="270"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
  },
  {
    id: 'leave-request',
    name: 'Leave Request',
    description: 'Employee leave request with supervisor and optional executive approval',
    category: 'HR',
    bpmn: `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://demo.com/bpm">
    <process id="leave-request" name="Leave Request" isExecutable="true">
        <documentation>Sequential leave request approval workflow</documentation>
        <startEvent id="startEvent" name="Leave Requested">
            <outgoing>flow1</outgoing>
        </startEvent>
        <sequenceFlow id="flow1" sourceRef="startEvent" targetRef="supervisorApproval"/>
        <userTask id="supervisorApproval" name="Review Leave Request"
                  flowable:candidateGroups="supervisors">
            <documentation>Review leave request</documentation>
            <incoming>flow1</incoming>
            <outgoing>flow2</outgoing>
        </userTask>
        <sequenceFlow id="flow2" sourceRef="supervisorApproval" targetRef="checkSupervisorDecision"/>
        <exclusiveGateway id="checkSupervisorDecision" name="Supervisor Decision">
            <incoming>flow2</incoming>
            <outgoing>flowShortApproved</outgoing>
            <outgoing>flowLongApproved</outgoing>
            <outgoing>flowRejected</outgoing>
        </exclusiveGateway>
        <sequenceFlow id="flowShortApproved" sourceRef="checkSupervisorDecision" targetRef="endApproved">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'approved' && days <= 5}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="flowLongApproved" sourceRef="checkSupervisorDecision" targetRef="executiveApproval">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'approved' && days > 5}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="flowRejected" sourceRef="checkSupervisorDecision" targetRef="endRejected">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'rejected'}]]></conditionExpression>
        </sequenceFlow>
        <userTask id="executiveApproval" name="Executive Leave Approval"
                  flowable:candidateGroups="executives">
            <documentation>Final approval for extended leave</documentation>
            <incoming>flowLongApproved</incoming>
            <outgoing>flow3</outgoing>
        </userTask>
        <sequenceFlow id="flow3" sourceRef="executiveApproval" targetRef="checkExecutiveDecision"/>
        <exclusiveGateway id="checkExecutiveDecision" name="Executive Decision">
            <incoming>flow3</incoming>
            <outgoing>flowExecApproved</outgoing>
            <outgoing>flowExecRejected</outgoing>
        </exclusiveGateway>
        <sequenceFlow id="flowExecApproved" sourceRef="checkExecutiveDecision" targetRef="endApproved">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'approved'}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="flowExecRejected" sourceRef="checkExecutiveDecision" targetRef="endRejected">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'rejected'}]]></conditionExpression>
        </sequenceFlow>
        <endEvent id="endApproved" name="Leave Approved"/>
        <endEvent id="endRejected" name="Leave Rejected"/>
    </process>
    <bpmndi:BPMNDiagram id="BPMNDiagram_leave-request">
        <bpmndi:BPMNPlane id="BPMNPlane_leave-request" bpmnElement="leave-request">
            <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
                <dc:Bounds x="100" y="200" width="36" height="36"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="supervisorApproval_di" bpmnElement="supervisorApproval">
                <dc:Bounds x="200" y="178" width="100" height="80"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="checkSupervisorDecision_di" bpmnElement="checkSupervisorDecision" isMarkerVisible="true">
                <dc:Bounds x="365" y="193" width="50" height="50"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="executiveApproval_di" bpmnElement="executiveApproval">
                <dc:Bounds x="500" y="80" width="100" height="80"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="checkExecutiveDecision_di" bpmnElement="checkExecutiveDecision" isMarkerVisible="true">
                <dc:Bounds x="665" y="95" width="50" height="50"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="endApproved_di" bpmnElement="endApproved">
                <dc:Bounds x="800" y="200" width="36" height="36"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="endRejected_di" bpmnElement="endRejected">
                <dc:Bounds x="800" y="320" width="36" height="36"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNEdge id="flow1_di" bpmnElement="flow1">
                <di:waypoint x="136" y="218"/>
                <di:waypoint x="200" y="218"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flow2_di" bpmnElement="flow2">
                <di:waypoint x="300" y="218"/>
                <di:waypoint x="365" y="218"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowShortApproved_di" bpmnElement="flowShortApproved">
                <di:waypoint x="415" y="218"/>
                <di:waypoint x="800" y="218"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowLongApproved_di" bpmnElement="flowLongApproved">
                <di:waypoint x="390" y="193"/>
                <di:waypoint x="390" y="120"/>
                <di:waypoint x="500" y="120"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowRejected_di" bpmnElement="flowRejected">
                <di:waypoint x="390" y="243"/>
                <di:waypoint x="390" y="338"/>
                <di:waypoint x="800" y="338"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flow3_di" bpmnElement="flow3">
                <di:waypoint x="600" y="120"/>
                <di:waypoint x="665" y="120"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowExecApproved_di" bpmnElement="flowExecApproved">
                <di:waypoint x="715" y="120"/>
                <di:waypoint x="818" y="120"/>
                <di:waypoint x="818" y="200"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowExecRejected_di" bpmnElement="flowExecRejected">
                <di:waypoint x="690" y="145"/>
                <di:waypoint x="690" y="338"/>
                <di:waypoint x="800" y="338"/>
            </bpmndi:BPMNEdge>
        </bpmndi:BPMNPlane>
    </bpmndi:BPMNDiagram>
</definitions>`
  },
  {
    id: 'expense-approval',
    name: 'Expense Approval',
    description: 'Expense approval with amount-based routing',
    category: 'Finance',
    bpmn: `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://demo.com/bpm">
    <process id="expense-approval" name="Expense Approval" isExecutable="true">
        <documentation>Expense approval workflow with threshold-based routing</documentation>
        <startEvent id="startEvent" name="Expense Submitted">
            <outgoing>flow1</outgoing>
        </startEvent>
        <sequenceFlow id="flow1" sourceRef="startEvent" targetRef="checkAmount"/>
        <exclusiveGateway id="checkAmount" name="Check Amount">
            <incoming>flow1</incoming>
            <outgoing>flowLow</outgoing>
            <outgoing>flowHigh</outgoing>
        </exclusiveGateway>
        <sequenceFlow id="flowLow" sourceRef="checkAmount" targetRef="supervisorApproval">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${amount <= 500}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="flowHigh" sourceRef="checkAmount" targetRef="supervisorReview">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${amount > 500}]]></conditionExpression>
        </sequenceFlow>
        <userTask id="supervisorApproval" name="Approve Expense"
                  flowable:candidateGroups="supervisors">
            <incoming>flowLow</incoming>
            <outgoing>flow2</outgoing>
        </userTask>
        <sequenceFlow id="flow2" sourceRef="supervisorApproval" targetRef="checkSupervisorDecision"/>
        <exclusiveGateway id="checkSupervisorDecision" name="Approved?">
            <incoming>flow2</incoming>
            <outgoing>flowApproved</outgoing>
            <outgoing>flowRejected</outgoing>
        </exclusiveGateway>
        <sequenceFlow id="flowApproved" sourceRef="checkSupervisorDecision" targetRef="endApproved">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'approved'}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="flowRejected" sourceRef="checkSupervisorDecision" targetRef="endRejected">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'rejected'}]]></conditionExpression>
        </sequenceFlow>
        <userTask id="supervisorReview" name="Review High-Value Expense"
                  flowable:candidateGroups="supervisors">
            <incoming>flowHigh</incoming>
            <outgoing>flow3</outgoing>
        </userTask>
        <sequenceFlow id="flow3" sourceRef="supervisorReview" targetRef="checkReviewDecision"/>
        <exclusiveGateway id="checkReviewDecision" name="Recommend?">
            <incoming>flow3</incoming>
            <outgoing>flowRecommend</outgoing>
            <outgoing>flowReviewReject</outgoing>
        </exclusiveGateway>
        <sequenceFlow id="flowRecommend" sourceRef="checkReviewDecision" targetRef="executiveApproval">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'approved'}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="flowReviewReject" sourceRef="checkReviewDecision" targetRef="endRejected">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'rejected'}]]></conditionExpression>
        </sequenceFlow>
        <userTask id="executiveApproval" name="Executive Approval Required"
                  flowable:candidateGroups="executives">
            <incoming>flowRecommend</incoming>
            <outgoing>flow4</outgoing>
        </userTask>
        <sequenceFlow id="flow4" sourceRef="executiveApproval" targetRef="checkExecutiveDecision"/>
        <exclusiveGateway id="checkExecutiveDecision" name="Executive Approved?">
            <incoming>flow4</incoming>
            <outgoing>flowExecApproved</outgoing>
            <outgoing>flowExecRejected</outgoing>
        </exclusiveGateway>
        <sequenceFlow id="flowExecApproved" sourceRef="checkExecutiveDecision" targetRef="endApproved">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'approved'}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="flowExecRejected" sourceRef="checkExecutiveDecision" targetRef="endRejected">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[\${decision == 'rejected'}]]></conditionExpression>
        </sequenceFlow>
        <endEvent id="endApproved" name="Expense Approved"/>
        <endEvent id="endRejected" name="Expense Rejected"/>
    </process>
    <bpmndi:BPMNDiagram id="BPMNDiagram_expense-approval">
        <bpmndi:BPMNPlane id="BPMNPlane_expense-approval" bpmnElement="expense-approval">
            <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
                <dc:Bounds x="100" y="200" width="36" height="36"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="checkAmount_di" bpmnElement="checkAmount" isMarkerVisible="true">
                <dc:Bounds x="195" y="193" width="50" height="50"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="supervisorApproval_di" bpmnElement="supervisorApproval">
                <dc:Bounds x="320" y="100" width="100" height="80"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="checkSupervisorDecision_di" bpmnElement="checkSupervisorDecision" isMarkerVisible="true">
                <dc:Bounds x="485" y="115" width="50" height="50"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="supervisorReview_di" bpmnElement="supervisorReview">
                <dc:Bounds x="320" y="280" width="100" height="80"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="checkReviewDecision_di" bpmnElement="checkReviewDecision" isMarkerVisible="true">
                <dc:Bounds x="485" y="295" width="50" height="50"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="executiveApproval_di" bpmnElement="executiveApproval">
                <dc:Bounds x="600" y="280" width="100" height="80"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="checkExecutiveDecision_di" bpmnElement="checkExecutiveDecision" isMarkerVisible="true">
                <dc:Bounds x="765" y="295" width="50" height="50"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="endApproved_di" bpmnElement="endApproved">
                <dc:Bounds x="900" y="200" width="36" height="36"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="endRejected_di" bpmnElement="endRejected">
                <dc:Bounds x="900" y="400" width="36" height="36"/>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNEdge id="flow1_di" bpmnElement="flow1">
                <di:waypoint x="136" y="218"/>
                <di:waypoint x="195" y="218"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowLow_di" bpmnElement="flowLow">
                <di:waypoint x="220" y="193"/>
                <di:waypoint x="220" y="140"/>
                <di:waypoint x="320" y="140"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowHigh_di" bpmnElement="flowHigh">
                <di:waypoint x="220" y="243"/>
                <di:waypoint x="220" y="320"/>
                <di:waypoint x="320" y="320"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flow2_di" bpmnElement="flow2">
                <di:waypoint x="420" y="140"/>
                <di:waypoint x="485" y="140"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowApproved_di" bpmnElement="flowApproved">
                <di:waypoint x="535" y="140"/>
                <di:waypoint x="918" y="140"/>
                <di:waypoint x="918" y="200"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowRejected_di" bpmnElement="flowRejected">
                <di:waypoint x="510" y="165"/>
                <di:waypoint x="510" y="418"/>
                <di:waypoint x="900" y="418"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flow3_di" bpmnElement="flow3">
                <di:waypoint x="420" y="320"/>
                <di:waypoint x="485" y="320"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowRecommend_di" bpmnElement="flowRecommend">
                <di:waypoint x="535" y="320"/>
                <di:waypoint x="600" y="320"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowReviewReject_di" bpmnElement="flowReviewReject">
                <di:waypoint x="510" y="345"/>
                <di:waypoint x="510" y="418"/>
                <di:waypoint x="900" y="418"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flow4_di" bpmnElement="flow4">
                <di:waypoint x="700" y="320"/>
                <di:waypoint x="765" y="320"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowExecApproved_di" bpmnElement="flowExecApproved">
                <di:waypoint x="790" y="295"/>
                <di:waypoint x="790" y="218"/>
                <di:waypoint x="900" y="218"/>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="flowExecRejected_di" bpmnElement="flowExecRejected">
                <di:waypoint x="790" y="345"/>
                <di:waypoint x="790" y="418"/>
                <di:waypoint x="900" y="418"/>
            </bpmndi:BPMNEdge>
        </bpmndi:BPMNPlane>
    </bpmndi:BPMNDiagram>
</definitions>`
  },
  {
    id: 'parallel-review',
    name: 'Parallel Review',
    description: 'Multiple reviewers work in parallel before final approval',
    category: 'Approval',
    bpmn: `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="parallel-review" name="Parallel Review" isExecutable="true">
    <bpmn:documentation>Parallel review workflow with multiple reviewers</bpmn:documentation>
    <bpmn:startEvent id="startEvent" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:parallelGateway id="splitGateway" name="Split">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>
      <bpmn:outgoing>Flow_4</bpmn:outgoing>
    </bpmn:parallelGateway>
    <bpmn:userTask id="technicalReview" name="Technical Review" flowable:candidateGroups="engineering">
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_5</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:userTask id="financialReview" name="Financial Review" flowable:candidateGroups="finance">
      <bpmn:incoming>Flow_3</bpmn:incoming>
      <bpmn:outgoing>Flow_6</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:userTask id="legalReview" name="Legal Review" flowable:candidateGroups="legal">
      <bpmn:incoming>Flow_4</bpmn:incoming>
      <bpmn:outgoing>Flow_7</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:parallelGateway id="joinGateway" name="Join">
      <bpmn:incoming>Flow_5</bpmn:incoming>
      <bpmn:incoming>Flow_6</bpmn:incoming>
      <bpmn:incoming>Flow_7</bpmn:incoming>
      <bpmn:outgoing>Flow_8</bpmn:outgoing>
    </bpmn:parallelGateway>
    <bpmn:userTask id="finalApproval" name="Final Approval" flowable:candidateGroups="executives">
      <bpmn:incoming>Flow_8</bpmn:incoming>
      <bpmn:outgoing>Flow_9</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="endEvent" name="End">
      <bpmn:incoming>Flow_9</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="startEvent" targetRef="splitGateway"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="splitGateway" targetRef="technicalReview"/>
    <bpmn:sequenceFlow id="Flow_3" sourceRef="splitGateway" targetRef="financialReview"/>
    <bpmn:sequenceFlow id="Flow_4" sourceRef="splitGateway" targetRef="legalReview"/>
    <bpmn:sequenceFlow id="Flow_5" sourceRef="technicalReview" targetRef="joinGateway"/>
    <bpmn:sequenceFlow id="Flow_6" sourceRef="financialReview" targetRef="joinGateway"/>
    <bpmn:sequenceFlow id="Flow_7" sourceRef="legalReview" targetRef="joinGateway"/>
    <bpmn:sequenceFlow id="Flow_8" sourceRef="joinGateway" targetRef="finalApproval"/>
    <bpmn:sequenceFlow id="Flow_9" sourceRef="finalApproval" targetRef="endEvent"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="parallel-review">
      <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
        <dc:Bounds x="100" y="192" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="splitGateway_di" bpmnElement="splitGateway">
        <dc:Bounds x="195" y="185" width="50" height="50"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="technicalReview_di" bpmnElement="technicalReview">
        <dc:Bounds x="320" y="80" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="financialReview_di" bpmnElement="financialReview">
        <dc:Bounds x="320" y="170" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="legalReview_di" bpmnElement="legalReview">
        <dc:Bounds x="320" y="260" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="joinGateway_di" bpmnElement="joinGateway">
        <dc:Bounds x="495" y="185" width="50" height="50"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="finalApproval_di" bpmnElement="finalApproval">
        <dc:Bounds x="620" y="170" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent">
        <dc:Bounds x="802" y="192" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="136" y="210"/>
        <di:waypoint x="195" y="210"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="220" y="185"/>
        <di:waypoint x="220" y="120"/>
        <di:waypoint x="320" y="120"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_3_di" bpmnElement="Flow_3">
        <di:waypoint x="245" y="210"/>
        <di:waypoint x="320" y="210"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_4_di" bpmnElement="Flow_4">
        <di:waypoint x="220" y="235"/>
        <di:waypoint x="220" y="300"/>
        <di:waypoint x="320" y="300"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_5_di" bpmnElement="Flow_5">
        <di:waypoint x="420" y="120"/>
        <di:waypoint x="520" y="120"/>
        <di:waypoint x="520" y="185"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_6_di" bpmnElement="Flow_6">
        <di:waypoint x="420" y="210"/>
        <di:waypoint x="495" y="210"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_7_di" bpmnElement="Flow_7">
        <di:waypoint x="420" y="300"/>
        <di:waypoint x="520" y="300"/>
        <di:waypoint x="520" y="235"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_8_di" bpmnElement="Flow_8">
        <di:waypoint x="545" y="210"/>
        <di:waypoint x="620" y="210"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_9_di" bpmnElement="Flow_9">
        <di:waypoint x="720" y="210"/>
        <di:waypoint x="802" y="210"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
  },
  {
    id: 'task-assignment',
    name: 'Task Assignment',
    description: 'Simple task assignment and completion workflow',
    category: 'Basic',
    bpmn: `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="task-assignment" name="Task Assignment" isExecutable="true">
    <bpmn:documentation>Simple task assignment workflow</bpmn:documentation>
    <bpmn:startEvent id="startEvent" name="Task Created">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="assignTask" name="Assign Task" flowable:candidateGroups="managers">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:userTask id="workOnTask" name="Work on Task" flowable:assignee="\${assignee}">
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:userTask id="reviewTask" name="Review Completion" flowable:candidateGroups="managers">
      <bpmn:incoming>Flow_3</bpmn:incoming>
      <bpmn:outgoing>Flow_4</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="endEvent" name="Task Completed">
      <bpmn:incoming>Flow_4</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="startEvent" targetRef="assignTask"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="assignTask" targetRef="workOnTask"/>
    <bpmn:sequenceFlow id="Flow_3" sourceRef="workOnTask" targetRef="reviewTask"/>
    <bpmn:sequenceFlow id="Flow_4" sourceRef="reviewTask" targetRef="endEvent"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="task-assignment">
      <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
        <dc:Bounds x="100" y="102" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="assignTask_di" bpmnElement="assignTask">
        <dc:Bounds x="200" y="80" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="workOnTask_di" bpmnElement="workOnTask">
        <dc:Bounds x="370" y="80" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="reviewTask_di" bpmnElement="reviewTask">
        <dc:Bounds x="540" y="80" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent">
        <dc:Bounds x="712" y="102" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="136" y="120"/>
        <di:waypoint x="200" y="120"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="300" y="120"/>
        <di:waypoint x="370" y="120"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_3_di" bpmnElement="Flow_3">
        <di:waypoint x="470" y="120"/>
        <di:waypoint x="540" y="120"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_4_di" bpmnElement="Flow_4">
        <di:waypoint x="640" y="120"/>
        <di:waypoint x="712" y="120"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
  },
  {
    id: 'product-price-routing',
    name: 'Product Price Routing',
    description:
      'Workflow with products grid - routes to different paths based on whether any product price exceeds threshold (>10)',
    category: 'Finance',
    bpmn: `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="product-price-routing" name="Product Price Routing" isExecutable="true">
    <bpmn:documentation>Product workflow with grid-based conditional routing. Routes to High Value Processing if any product price exceeds 10, otherwise routes to Standard Processing.</bpmn:documentation>
    <bpmn:startEvent id="startEvent" name="Start Order">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="enterProducts" name="Enter Products" flowable:candidateGroups="sales"
                   flowable:formGrids='[{"id":"grid_products","name":"products","label":"Products","description":"Enter product details","minRows":1,"maxRows":20,"columns":[{"id":"col_name","name":"name","label":"Product Name","type":"text","required":true,"placeholder":"Enter product name","options":null,"validation":null},{"id":"col_price","name":"price","label":"Price","type":"number","required":true,"placeholder":"0.00","options":null,"min":0,"step":0.01,"validation":null},{"id":"col_quantity","name":"quantity","label":"Quantity","type":"number","required":true,"placeholder":"1","options":null,"min":1,"step":1,"validation":null}],"gridColumn":1,"gridRow":1,"gridWidth":12,"cssClass":""}]'>
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:scriptTask id="evaluateProducts" name="Evaluate Product Prices" scriptFormat="javascript">
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>
      <bpmn:script><![CDATA[
// Check if any product in the grid has price > 10
var hasHighPriceItem = false;
var products = execution.getVariable('products');
if (products != null) {
  for (var i = 0; i < products.size(); i++) {
    var product = products.get(i);
    var price = product.get('price');
    if (price != null && price > 10) {
      hasHighPriceItem = true;
      break;
    }
  }
}
execution.setVariable('hasHighPriceItem', hasHighPriceItem);
      ]]></bpmn:script>
    </bpmn:scriptTask>
    <bpmn:exclusiveGateway id="priceCheckGateway" name="High Price Item?">
      <bpmn:incoming>Flow_3</bpmn:incoming>
      <bpmn:outgoing>Flow_highValue</bpmn:outgoing>
      <bpmn:outgoing>Flow_standard</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:userTask id="highValueProcessing" name="High Value Processing" flowable:candidateGroups="managers">
      <bpmn:documentation>Process order with high-value items (price > 10) - requires manager approval</bpmn:documentation>
      <bpmn:incoming>Flow_highValue</bpmn:incoming>
      <bpmn:outgoing>Flow_4</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:userTask id="standardProcessing" name="Standard Processing" flowable:candidateGroups="sales">
      <bpmn:documentation>Process standard order (all items price ≤ 10)</bpmn:documentation>
      <bpmn:incoming>Flow_standard</bpmn:incoming>
      <bpmn:outgoing>Flow_5</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="endHighValue" name="High Value Completed">
      <bpmn:incoming>Flow_4</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:endEvent id="endStandard" name="Standard Completed">
      <bpmn:incoming>Flow_5</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="startEvent" targetRef="enterProducts"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="enterProducts" targetRef="evaluateProducts"/>
    <bpmn:sequenceFlow id="Flow_3" sourceRef="evaluateProducts" targetRef="priceCheckGateway"/>
    <bpmn:sequenceFlow id="Flow_highValue" name="Price > 10" sourceRef="priceCheckGateway" targetRef="highValueProcessing">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${hasHighPriceItem == true}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_standard" name="All Prices ≤ 10" sourceRef="priceCheckGateway" targetRef="standardProcessing">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${hasHighPriceItem == false}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_4" sourceRef="highValueProcessing" targetRef="endHighValue"/>
    <bpmn:sequenceFlow id="Flow_5" sourceRef="standardProcessing" targetRef="endStandard"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="product-price-routing">
      <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
        <dc:Bounds x="100" y="192" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="88" y="235" width="60" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="enterProducts_di" bpmnElement="enterProducts">
        <dc:Bounds x="200" y="170" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="evaluateProducts_di" bpmnElement="evaluateProducts">
        <dc:Bounds x="370" y="170" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="priceCheckGateway_di" bpmnElement="priceCheckGateway" isMarkerVisible="true">
        <dc:Bounds x="545" y="185" width="50" height="50"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="527" y="242" width="86" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="highValueProcessing_di" bpmnElement="highValueProcessing">
        <dc:Bounds x="680" y="80" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="standardProcessing_di" bpmnElement="standardProcessing">
        <dc:Bounds x="680" y="260" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="endHighValue_di" bpmnElement="endHighValue">
        <dc:Bounds x="862" y="102" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="841" y="145" width="78" height="27"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="endStandard_di" bpmnElement="endStandard">
        <dc:Bounds x="862" y="282" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="841" y="325" width="78" height="27"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="136" y="210"/>
        <di:waypoint x="200" y="210"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="300" y="210"/>
        <di:waypoint x="370" y="210"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_3_di" bpmnElement="Flow_3">
        <di:waypoint x="470" y="210"/>
        <di:waypoint x="545" y="210"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_highValue_di" bpmnElement="Flow_highValue">
        <di:waypoint x="570" y="185"/>
        <di:waypoint x="570" y="120"/>
        <di:waypoint x="680" y="120"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="585" y="103" width="50" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_standard_di" bpmnElement="Flow_standard">
        <di:waypoint x="570" y="235"/>
        <di:waypoint x="570" y="300"/>
        <di:waypoint x="680" y="300"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="578" y="283" width="78" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_4_di" bpmnElement="Flow_4">
        <di:waypoint x="780" y="120"/>
        <di:waypoint x="862" y="120"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_5_di" bpmnElement="Flow_5">
        <di:waypoint x="780" y="300"/>
        <di:waypoint x="862" y="300"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
  }
];

export function getDemoProcessById(id: string): DemoProcess | undefined {
  return demoProcesses.find((p) => p.id === id);
}

export function getDemoProcessesByCategory(): Map<string, DemoProcess[]> {
  const grouped = new Map<string, DemoProcess[]>();
  for (const process of demoProcesses) {
    const existing = grouped.get(process.category) || [];
    existing.push(process);
    grouped.set(process.category, existing);
  }
  return grouped;
}
