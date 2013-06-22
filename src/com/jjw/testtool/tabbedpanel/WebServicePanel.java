package com.jjw.testtool.tabbedpanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.springframework.web.client.RestTemplate;

import com.jjw.webservice.pojo.Person;

public class WebServicePanel extends JPanel
{
    /** Logger instance. */
    Logger LOG = Logger.getLogger(WebServicePanel.class);

    /** Serial ID */
    private static final long serialVersionUID = 1L;

    /** Base URL to the web services */
    private static final String URL_BASE = "http://localhost:8080/com.jjw.webservice/";

    /** Text field to specify the ID to pass to the service */
    private JTextField myIdTextField;

    /** Interface to REST services */
    private final RestTemplate myRestTemplate;

    public WebServicePanel()
    {
        super();

        myRestTemplate = new RestTemplate();

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
        southPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton getButton = new JButton("Send GET");
        getButton.addActionListener(new GetActionListener());

        myIdTextField = new JTextField("ID");

        southPanel.add(getButton);
        southPanel.add(myIdTextField);
        this.add(southPanel, BorderLayout.SOUTH);
    }

    private void setupWestPanel()
    {
        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.Y_AXIS));

        this.add(westPanel, BorderLayout.WEST);
    }

    private class GetActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {

            try
            {
                LOG.info("Sending request to " + URL_BASE + "addressbook/" + myIdTextField.getText());

                Map<String, String> vars = new HashMap<String, String>();
                vars.put("id", myIdTextField.getText());
                Person person = myRestTemplate.getForObject(URL_BASE + "addressbook/{id}", Person.class, vars);

                LOG.info("Received person: " + person);
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }
        }
    }
}
