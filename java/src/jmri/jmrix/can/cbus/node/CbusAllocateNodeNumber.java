package jmri.jmrix.can.cbus.node;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;

import jmri.jmrix.can.CanListener;
import jmri.jmrix.can.CanMessage;
import jmri.jmrix.can.CanSystemConnectionMemo;
import jmri.jmrix.can.CanReply;
import jmri.jmrix.can.TrafficController;
import jmri.jmrix.can.cbus.CbusConstants;
import jmri.jmrix.can.cbus.CbusMessage;
import jmri.jmrix.can.cbus.CbusSend;
import jmri.jmrix.can.cbus.node.CbusNode;
import jmri.jmrix.can.cbus.node.CbusNodeConstants;
import jmri.jmrix.can.cbus.node.CbusNodeTableDataModel;
import java.util.TimerTask;
import jmri.util.TimerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CbusAllocateNodeNumber implements CanListener {
    
    public CbusNodeTableDataModel nodeModel=null;    
    private TrafficController tc;
    CbusSend send;
    
    JLabel rqNNtext;
    int baseNodeNum;
    Boolean WAITINGRESPONSE_RQNN_PARAMS = false;
    Boolean NODE_NUM_DIALOGUE_OPEN = false;
    int[] _paramsArr;
    
    public CbusAllocateNodeNumber(CanSystemConnectionMemo memo, CbusNodeTableDataModel model) {
        
        nodeModel = model;
        
        // connect to the CanInterface
        tc = memo.getTrafficController();
        if (tc != null ) {
            tc.addCanListener(this);
        }
        
        send = new CbusSend(memo);
        
        baseNodeNum = 256;
        _paramsArr = null;
        
    }
    
    private void startnodeallocation(int nn, String nodeText) {
        
        if (NODE_NUM_DIALOGUE_OPEN) {
            return;
        }
        
        NODE_NUM_DIALOGUE_OPEN=true;
        
        JPanel rqNNpane = new JPanel();
        JPanel bottomrqNNpane = new JPanel();
        String spinnerlabel=Bundle.getMessage("NdRqNnSelect");
        JLabel rqNNspinnerlabel = new JLabel(spinnerlabel);
        
        bottomrqNNpane.setLayout(new GridLayout(2, 1));
        rqNNpane.setLayout(new BorderLayout());
        rqNNtext = new JLabel(Bundle.getMessage("NdRqNdDetails"));
        
        String popuplabel;
        
        if (nn==0) {
            popuplabel=Bundle.getMessage("NdEntrSlimTitle");
             _paramsArr = null; // reset just in case
        } 
        else if ( nn==-1 ){
            popuplabel="Node found in Setup Mode";
            
            if ( nodeText != null ) {
                rqNNtext.setText(nodeText);
            }
            
        }
        else {
            popuplabel=Bundle.getMessage("NdEntrNumTitle",nn);
             _paramsArr = null; // reset just in case
        }
     
        baseNodeNum =  nodeModel.getNextAvailableNodeNumber(baseNodeNum);
        
        JSpinner rqnnSpinner = new JSpinner(new SpinnerNumberModel(baseNodeNum, 1, 65535, 1));
        rqnnSpinner.setToolTipText((Bundle.getMessage("ToolTipNodeNumber")));
        JComponent rqcomp = rqnnSpinner.getEditor();
        JFormattedTextField rqfield = (JFormattedTextField) rqcomp.getComponent(0);
        DefaultFormatter rqformatter = (DefaultFormatter) rqfield.getFormatter();
        rqformatter.setCommitsOnValidEdit(true);
        rqfield.setBackground(Color.white);
        rqnnSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newval = (Integer) rqnnSpinner.getValue();
              
                if (!CbusNodeConstants.getReservedModule(newval).isEmpty()) {
                    rqNNspinnerlabel.setText(CbusNodeConstants.getReservedModule(newval));
                    rqfield.setBackground(Color.yellow);
                }
                else {
                    rqNNspinnerlabel.setText(spinnerlabel);
                    rqfield.setBackground(Color.white);
                }
                if ( !nodeModel.getNodeNumberName(newval).isEmpty() ) {
                    rqNNspinnerlabel.setText(Bundle.getMessage("NdNumInUse",nodeModel.getNodeNumberName(newval)));
                    rqfield.setBackground(Color.red);
                }
             
            }
        });
        
        rqNNpane.add(rqNNtext, BorderLayout.CENTER);
        bottomrqNNpane.add(rqNNspinnerlabel);
        bottomrqNNpane.add(rqnnSpinner);
        
        rqNNpane.add(bottomrqNNpane, BorderLayout.PAGE_END);
        
        Toolkit.getDefaultToolkit().beep();
        
        if ( _paramsArr==null ) {
        
            WAITINGRESPONSE_RQNN_PARAMS=true;
            send.nodeRequestParamSetup();
        
        }
        
        int option = JOptionPane.showOptionDialog(null, 
            rqNNpane, 
            popuplabel, 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (option == JOptionPane.CANCEL_OPTION) {

            WAITINGRESPONSE_RQNN_PARAMS=false;
            NODE_NUM_DIALOGUE_OPEN=false;
        } else if (option == JOptionPane.OK_OPTION) {
            int newval = (Integer) rqnnSpinner.getValue();
            baseNodeNum = newval;
            NODE_NUM_DIALOGUE_OPEN=false;
            setSendSNNTimeout();
            send.nodeSetNodeNumber(newval);
        }
    }
    
    TimerTask sendSNNTask;
    
    void clearSendSNNTimeout(){
        if (sendSNNTask != null ) {
            sendSNNTask.cancel();
            sendSNNTask = null;
        }
    }
    
    void setSendSNNTimeout() {
        sendSNNTask = new TimerTask() {
            @Override
            public void run() {
                sendSNNTask = null;
                log.warn("No confirmation from node when setting node number {}", baseNodeNum );
                JOptionPane.showMessageDialog(null, 
                    Bundle.getMessage("NnAllocError",baseNodeNum), Bundle.getMessage("WarningTitle"),
                    JOptionPane.ERROR_MESSAGE);
                clearSendSNNTimeout();
                NODE_NUM_DIALOGUE_OPEN=false;
            }
        };
        TimerUtil.schedule(sendSNNTask, CbusNode.SINGLE_MESSAGE_TIMEOUT_TIME);
    }
    
    /**
     * If popup not open send a setup param request to try and catch nodes awaiting number allocation
     * when an all node respond message is sent
     * @param m canmessage
     */
    @Override
    public void message(CanMessage m) { // outgoing cbus message
        if (CbusMessage.getOpcode(m) == CbusConstants.CBUS_QNN) {
            if (!NODE_NUM_DIALOGUE_OPEN) {
                send.nodeRequestParamSetup();
            }
        }
    }
    
    /**
     * Capture node and event, check isevent and send to parse from reply.
     * @param m canmessage
     */
    @Override
    public void reply(CanReply m) { // incoming cbus message
        int opc = CbusMessage.getOpcode(m);

        if (opc==CbusConstants.CBUS_RQNN){ // node requesting a number, nn is existing number
            _paramsArr = null;
            startnodeallocation( ( m.getElement(1) * 256 ) + m.getElement(2), null );
        }
        
        if (opc==CbusConstants.CBUS_PARAMS) {
            
            StringBuilder nodepropbuilder = new StringBuilder(40);
            nodepropbuilder.append (CbusNodeConstants.getManu(m.getElement(1)));  
            nodepropbuilder.append (" ");
            nodepropbuilder.append( CbusNodeConstants.getModuleType(m.getElement(1),m.getElement(3)));
            
            _paramsArr = new int[] { m.getElement(1),m.getElement(2),m.getElement(3),m.getElement(4),
                m.getElement(5),m.getElement(6),m.getElement(7) };
            
            if (WAITINGRESPONSE_RQNN_PARAMS) {
                rqNNtext.setText(nodepropbuilder.toString());
                WAITINGRESPONSE_RQNN_PARAMS=false;
            }
            else if (!NODE_NUM_DIALOGUE_OPEN) {
                startnodeallocation( -1, nodepropbuilder.toString() );
            }
        }
        
        if (opc==CbusConstants.CBUS_NNACK) {
            log.debug("Node confirms its node number"); // sent from JMRI or other method
            clearSendSNNTimeout();
            
            // if nodes are allowed to be added to node table, add.
            // this is done here so any known parameters can be passed directly rather than re-requested
            if ( jmri.InstanceManager.getDefault(jmri.jmrix.can.cbus.CbusPreferences.class).getAddNodes() ) {
                
                // provide will add to table
                CbusNode nd = nodeModel.provideNodeByNodeNum( ( m.getElement(1) * 256 ) + m.getElement(2) );
                
                nd.setParamsFromSetup(_paramsArr);
                
            }
            
            _paramsArr = null;
            
        }
        
    }

    public void dispose(){
        if (tc != null ) {
            tc.removeCanListener(this);
        }
    }

    private final static Logger log = LoggerFactory.getLogger(CbusAllocateNodeNumber.class);

}
