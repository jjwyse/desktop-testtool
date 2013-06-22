package com.jjw.testtool.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.jjw.testtool.tabbedpanel.ActiveMqPanel;
import com.jjw.testtool.tabbedpanel.WebServicePanel;

/**
 * TestTool GUI
 * 
 * @author jjwyse
 */
public class MainFrame extends JFrame
{
    /** Serial ID. */
    private static final long serialVersionUID = 1L;

    /** Logger instance. */
    Logger LOG = Logger.getLogger(MainFrame.class);

    @Autowired
    public ProducerTemplate myProducer;

    /**
     * 
     */
    public MainFrame()
    {
        super();
    }

    public void init()
    {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem menuItem = new JMenuItem("Exit");
        menuItem.addActionListener(new ExitListener());
        menu.add(menuItem);
        menuBar.add(menu);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("ActiveMQ", new ActiveMqPanel(myProducer));
        tabs.addTab("Web Service", new WebServicePanel());

        this.setJMenuBar(menuBar);
        this.add(tabs);
        this.pack();
        this.setVisible(true);
    }

    /**
     * Listens for an Exit click
     * 
     * @author jjwyse
     * 
     */
    private class ExitListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent arg0)
        {
            LOG.info("Exiting the application");
            System.exit(0);
        }
    }
}
