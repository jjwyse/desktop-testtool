package com.jjw.testtool.tabbedpanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;

public class JmsPanel extends JPanel
{
    /** Logger instance. */
    Logger LOG = Logger.getLogger(JmsPanel.class);

    /** Serial ID */
    private static final long serialVersionUID = 1L;

    public ProducerTemplate myProducer;

    public JmsPanel(ProducerTemplate producer)
    {
        super();

        myProducer = producer;

        setupLayout();

        setupSouthPanel();

        setupWestPanel();
    }

    private void setupLayout()
    {
        this.setLayout(new BorderLayout(10, 10));
    }

    private void setupSouthPanel()
    {
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        ButtonGroup buttonGroup = new ButtonGroup();
        final JRadioButton queueButton = new JRadioButton("Queue");
        final JRadioButton topicButton = new JRadioButton("Topic");
        final JRadioButton directButton = new JRadioButton("Direct");

        final JTextField queueTextField = new JTextField(10);
        final JTextField topicTextField = new JTextField(10);
        final JTextField directTextField = new JTextField(10);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                if (queueButton.isSelected())
                {
                    LOG.info("Sending Queue to jms:queue:" + queueTextField.getText());
                    myProducer.requestBody("jms:queue:" + queueTextField.getText(), "Queue");
                }
                else if (topicButton.isSelected())
                {
                    LOG.info("Sending Topic to jms:topic:" + topicTextField.getText());
                    myProducer.sendBody("jms:topic:" + topicTextField.getText(), "Topic");
                }
                else
                {
                    LOG.info("Sending Direct to direct:" + directTextField.getText());
                    myProducer.sendBody("direct:" + directTextField.getText(), "Direct");
                }
            }

        });

        buttonGroup.add(queueButton);
        buttonGroup.add(topicButton);
        buttonGroup.add(directButton);

        southPanel.add(queueButton);
        southPanel.add(queueTextField);
        southPanel.add(topicButton);
        southPanel.add(topicTextField);
        southPanel.add(directButton);
        southPanel.add(directTextField);
        southPanel.add(sendButton);

        this.add(southPanel, BorderLayout.SOUTH);
    }

    private void setupWestPanel()
    {
        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.Y_AXIS));

        this.add(westPanel, BorderLayout.WEST);
    }

}
