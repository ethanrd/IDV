/*
 * $Id: UrlChooser.java,v 1.40 2007/07/27 13:53:08 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.idv.chooser;


import org.w3c.dom.Element;

import ucar.unidata.data.DataManager;



import ucar.unidata.idv.*;




import ucar.unidata.ui.DateTimePicker;
import ucar.unidata.ui.ChooserPanel;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.TimeZone;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;


import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


import ucar.unidata.view.geoloc.NavigatedMapPanel;

import ucar.unidata.view.station.StationLocationMap;
import ucar.unidata.geoloc.*;
import ucar.unidata.view.geoloc.*;
import ucar.unidata.view.geoloc.NavigatedMapPanel;


/**
 * Allows the user to select a url as a data source
 *
 * @author IDV development team
 * @version $Revision: 1.40 $Date: 2007/07/27 13:53:08 $
 */


public class MesoWestChooser extends IdvChooser implements ActionListener {


    public static final String BASEURL = "http://mesowest.utah.edu/cgi-bin/droman/obs_lsa_export.cgi";

    public static final String ARG_CLAT = "clat";
    public static final String ARG_CLON = "clon";
    public static final String ARG_BOXRAD = "boxrad";
    public static final String ARG_HOUR1 = "hour1";
    public static final String ARG_DAY1 = "day1";
    public static final String ARG_MONTH1 = "month1";
    public static final String ARG_YEAR1 = "year1";

    private JTextField clatFld;
    private JTextField clonFld;
    private JComboBox radiiCbx;
    private DateTimePicker dateTimePicker;
    private NavigatedMapPanel map;

    /**
     * Create the <code>UrlChooser</code>
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public MesoWestChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    public boolean canDoUpdate() {
        return false;
    }


    /**
     * Get the tooltip for the load button
     *
     * @return The tooltip for the load button
     */
    protected String getLoadToolTip() {
        return "Load the MesoWest Data";
    }



    private void doAnnotateMap(Graphics2D g) {
        NavigatedPanel np = map.getNavigatedPanel();
        List<LatLonPoint> points  = new ArrayList<LatLonPoint>();
        LatLonRect llr = np.getSelectedEarthRegion();
        if(llr == null) {
            return;
        }
        double width  = llr.getWidth();
        double radii = Math.min(5,width/2);
        double clat = (llr.getLatMin()+(llr.getLatMax()-llr.getLatMin())/2);        
        double clon = (llr.getLonMin()+(llr.getLonMax()-llr.getLonMin())/2);
        points.add(new LatLonPointImpl(clat+radii, clon-radii));
        points.add(new LatLonPointImpl(clat+radii, clon+radii));
        points.add(new LatLonPointImpl(clat-radii, clon+radii));
        points.add(new LatLonPointImpl(clat-radii, clon-radii));

        g.setStroke(new BasicStroke(2.0f));
        g.setColor(Color.red);
        g.fillRect(0,0,1000,1000);
        //        g.drawLine(0,0,1000,1000);
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,
                                           points.size());
        for (int i = 0; i <= points.size(); i++) {
            ProjectionPoint ppi;
            LatLonPoint     llp;
            if (i >= points.size()) {
                llp =  points.get(0);
            } else {
                llp =  points.get(i);
            }
            Point2D p=  np.earthToScreen(llp);
            //            System.err.println ("\t" + p);
            if (i == 0) {
                path.moveTo((float) p.getX(), (float) p.getY());
            } else {
                path.lineTo((float) p.getX(), (float) p.getY());
            }
        }
        g.setColor(Color.red);
        g.draw(path);
    }


    /**
     * Create the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        map = new NavigatedMapPanel(true,true) {
                protected void annotateMap(Graphics2D g) {
                    super.annotateMap(g);
                    doAnnotateMap(g);
                }
            };
        NavigatedPanel np = map.getNavigatedPanel();
        np.setSelectRegionMode(true);
        np.setSelectedRegion(new LatLonRect(new LatLonPointImpl(39,-110),
                                            new LatLonPointImpl(43,-114)));

        try {

        ProjectionImpl proj = (ProjectionImpl)getIdv().decodeObject("<object class=\"ucar.unidata.geoloc.projection.LatLonProjection\"><property name=\"CenterLon\"><double>-109</double></property><property name=\"Name\"><string><![CDATA[US>States>West>Colorado]]></string></property><property name=\"DefaultMapArea\"><object class=\"ucar.unidata.geoloc.ProjectionRect\"><constructor><double>-124</double><double>31</double><double>-100</double><double>47</double></constructor></object></property></object>");

        np.setProjectionImpl(proj);
        } catch(Exception exc) {
            throw new RuntimeException(exc);
        }

        np.setPreferredSize(new Dimension(400,400));
        //        StationLocationMap map = getStationMap();
        dateTimePicker = new DateTimePicker();

        clatFld = new JTextField("",5);
        clonFld = new JTextField("",5);
        List<TwoFacedObject> radii = new ArrayList<TwoFacedObject>();
        radii.add(new TwoFacedObject("1 deg","1"));
        radii.add(new TwoFacedObject("2 deg","2"));
        radii.add(new TwoFacedObject("3 deg","3"));
        radii.add(new TwoFacedObject("4 deg","4"));
        radii.add(new TwoFacedObject("5 deg","5"));
        radiiCbx = new JComboBox(new Vector(radii));
        List comps = Misc.newList(GuiUtils.rLabel("Analysis Center Latitude:"),
                                  GuiUtils.left(clatFld),
                                  GuiUtils.rLabel("Analysis Center Longitude:"),
                                  GuiUtils.left(clonFld));
        comps = new ArrayList();
        comps.add(GuiUtils.rLabel("Date/Time:"));
        comps.add(GuiUtils.left(dateTimePicker));
        comps = Misc.newList(GuiUtils.rLabel("Location:"),   GuiUtils.centerBottom(np,GuiUtils.left(np.getNavToolBar())));
        //        comps.add(GuiUtils.rLabel("Analysis Domain Radius:"));
        //        comps.add(GuiUtils.left(radiiCbx));



        JComponent mainContents = GuiUtils.formLayout(comps,true);
        JComponent urlButtons = getDefaultButtons();
        setHaveData(true);
        return GuiUtils.top(GuiUtils.vbox(mainContents,urlButtons));
    }



    public void setStatus(String msg, String what) {
        super.setStatus("Press \"" + CMD_LOAD
                        + "\" to load the data", "buttons");
    }


    /**
     * Handle the action event from the GUI
     */
    public void doLoadInThread() {
        Hashtable properties   = new Hashtable();
        String    dataSourceId = "FILE.POINTTEXT";
        properties.put(DataManager.DATATYPE_ID, dataSourceId);
        Date date = dateTimePicker.getDate();
        GregorianCalendar cal = new GregorianCalendar(DateTimePicker.getDefaultTimeZone());
        cal.setTime(date);

        String hour = ""+cal.get(Calendar.HOUR_OF_DAY);
        String day = ""+cal.get(Calendar.DAY_OF_MONTH);
        String month = StringUtil.padLeft(""+(cal.get(Calendar.MONTH)+1),2,"0");
        String year = ""+cal.get(Calendar.YEAR);

        NavigatedPanel np = map.getNavigatedPanel();
        LatLonRect llr = np.getSelectedEarthRegion();
        if(llr == null) {
            LogUtil.userErrorMessage("You need to select a region");
            return;
        }

        double width  = llr.getWidth();
        double centerLon = llr.getCenterLon();
        //        System.out.println("llr:" + llr);
        //        System.out.println("minmax" + llr.getLatMin() +  " " + llr.getLatMax() + " " +llr.getLonMin() + " " + llr.getLonMax());
        double radii = Math.min(5,width/2);
        String url = HtmlUtil.url(BASEURL,
            new String[]{
                ARG_CLAT, (llr.getLatMin()+(llr.getLatMax()-llr.getLatMin())/2)+"",
                ARG_CLON, (llr.getLonMin()+(llr.getLonMax()-llr.getLonMin())/2)+"",
                ARG_BOXRAD,  radii+"",
                ARG_HOUR1,hour,
                ARG_DAY1,day,
                ARG_MONTH1,month,
                ARG_YEAR1,year});
        //        System.out.println(url);

        if(makeDataSource(url, dataSourceId, properties)) {
            closeChooser();
        }
    }



}

