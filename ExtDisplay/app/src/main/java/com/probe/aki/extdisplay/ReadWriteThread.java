package com.probe.aki.extdisplay;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ReadWriteThread extends Thread{
    private final BluetoothSocket bluetoothSocket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final String apikey;

    public ReadWriteThread(BluetoothSocket socket,String apikey) {
        Log.d("AML", "AML startattiin.. ReadWriteThread");
        this.bluetoothSocket = socket;
        this.apikey = apikey;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        inputStream = tmpIn;
        outputStream = tmpOut;
    }

    public void run() {
        while (true) {
            try {

                inputStream.read();

                byte b[] = new byte[]{0,0,0,0,0,0,0,0,0,0,0};
                Date d = new Date();
                b[9] = (byte)d.getHours();
                b[10] = (byte)d.getMinutes();
                try {
                    Log.d("AML", "AML luettiin harmaja");
                    byte t[] = readXMLobservations("100996");
                    b[0] = t[0];
                    b[1] = t[1];
                    b[2] = readXMLforecast("Harmaja,Helsinki");
                    Log.d("AML", "AML luettiin kumpula");
                    t = readXMLobservations("101004");
                    b[3] = t[0];
                    b[4] = t[1];
                    b[5] = readXMLforecast("Kumpula,Helsinki");
                    Log.d("AML", "AML luettiin vuosaari");
                    t = readXMLobservations("151028");
                    b[6] = t[0];
                    b[7] = t[1];
                    b[8] = '*';
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("AML", "AML lahetti ReadWriteThread");
                outputStream.write(b);

            } catch (IOException e) {
                Log.d("AML", "AML yhteys katkesi ReadWriteThread");
                break;
            }
        }
    }

    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] readXMLobservations(String place) throws Exception {
        Vector time = new Vector();
        Vector v = readWeather("http://data.fmi.fi/fmi-apikey/" + apikey + "/wfs?request=getFeature&storedquery_id=fmi::observations::weather::timevaluepair&fmisid="
                + place + "&parameters=windspeedms",time);

        byte b[] = new byte[2];
        double f1 = Double.parseDouble((String)v.get(v.size()-1));
        b[0] = (byte)(f1 + 0.5);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SS");

        Date d1 = df.parse((String)time.get(time.size()-1));
        for(int i = time.size()-1 ; i > 0 ; i--){
            Date d2 = df.parse((String)time.get(i));
            if((d1.getTime() - d2.getTime()) >= 1200000){
                double f2 = Double.parseDouble((String)v.get(i));

                Log.d("AML", "AML Mitattu Aika d1="+time.get(time.size()-1)+" d2="+time.get(i));
                Log.d("AML", "AML Mitattu Arvo f1="+f1+" f2="+f2);

                if(Math.abs(f1 - f2) < 1.0)
                    b[1] =  '*';
                else if(f1 > f2)
                    b[1] =  '+';
                else b[1] = '-';
                break;
            }
        }
        return b;
    }

    private byte readXMLforecast(String place) throws Exception {
        Vector time = new Vector();
        Vector v = readWeather("http://data.fmi.fi/fmi-apikey/" + apikey + "/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::timevaluepair&place="
                + place + "&parameters=windspeedms",time);

        double f1 = Double.parseDouble((String)v.get(0));
        double f2 = Double.parseDouble((String)v.get(1));

        Log.d("AML", "AML Ennuste f1="+f1+" f2="+f2);

        Log.d("AML", "AML Ennuste Math.abs(f1 - f2) "+Math.abs(f1 - f2));
        if(Math.abs(f1 - f2) < 1.0)
            return '*';
        else if(f1 > f2)
            return '-';
        return '+';
    }

    private Vector readWeather(String input, Vector time) throws Exception{

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc = db.parse(input);

        doc.getDocumentElement().normalize();
        NodeList nodeLst = doc.getElementsByTagName("wfs:member");
        return readMember((Element) nodeLst.item(0),time);
    }

    private Vector readMember(Element element, Vector time) {
        Vector v = new Vector();
        NodeList nodeLst = element.getElementsByTagName("omso:PointTimeSeriesObservation");
        element = (Element) nodeLst.item(0);
        nodeLst = element.getElementsByTagName("om:result");
        element = (Element) nodeLst.item(0);
        nodeLst = element.getElementsByTagName("wml2:MeasurementTimeseries");
        element = (Element) nodeLst.item(0);

        NodeList nodeLst1 = element.getElementsByTagName("wml2:point");

        for (int k = 0; k < nodeLst1.getLength(); k++) {
            Element element1 = (Element) nodeLst1.item(k);

            NodeList nodeLst2 = element1.getElementsByTagName("wml2:MeasurementTVP");
            Element element2 = (Element) nodeLst2.item(0);

            if(time != null) {
                nodeLst2 = element2.getElementsByTagName("wml2:time");
                element1 = (Element) nodeLst2.item(0);
                nodeLst2 = element1.getChildNodes();
                time.add(((Node) nodeLst2.item(0)).getNodeValue());
            }
            nodeLst2 = element2.getElementsByTagName("wml2:value");
            element1 = (Element) nodeLst2.item(0);
            nodeLst2 = element1.getChildNodes();

            v.add(((Node) nodeLst2.item(0)).getNodeValue());
        }
        return v;
    }
}
