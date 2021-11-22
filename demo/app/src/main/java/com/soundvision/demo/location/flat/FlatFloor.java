package com.soundvision.demo.location.flat;


import java.util.List;

public abstract class FlatFloor
{
    public int id;
    public abstract Object getProps();
}

/*
public class Venue implements FlatFloor {
public List<int[]> beacons;
public List<Floor> floors;
public double lat;
public double lon;
public double angle;
public double mapOffset;
public String name;
public int singleRoomMajor;
public String uuid;

@Override
public Object getProps()
        {
        VenueProp p = new VenueProp();
        return p;
        }
}

*/
/*
//C#[DefaultPropertyAttribute("SaveOnClose")]
public class VenueProp2
{
    private bool saveOnClose = true;
    private String greetingText = "Welcome to your application!";
    private int maxRepeatRate = 10;
    private int itemsInMRU = 4;

    private bool settingsChanged = false;
    private String appVersion = "1.0";

        [CategoryAttribute("Document Settings"), DefaultValueAttribute(true)]
    public bool SaveOnClose
    {
        get { return saveOnClose; }
        set { saveOnClose = value; }
    }

        [CategoryAttribute("Global Settings"), ReadOnlyAttribute(true), DefaultValueAttribute("Welcome to your application!")]
    public String GreetingText
    {
        get { return greetingText; }
        set { greetingText = value; }
    }

        [CategoryAttribute("Global Settings"), DefaultValueAttribute(4)]
    public int ItemsInMRUList
    {
        get { return itemsInMRU; }
        set { itemsInMRU = value; }
    }

        [DescriptionAttribute("The rate in milliseconds that the text will repeat."), CategoryAttribute("Global Settings"), DefaultValueAttribute(10)]
    public int MaxRepeatRate
    {
        get { return maxRepeatRate; }
        set { maxRepeatRate = value; }
    }

        [BrowsableAttribute(false), DefaultValueAttribute(false)]
    public bool SettingsChanged
    {
        get { return settingsChanged; }
        set { settingsChanged = value; }
    }

        [CategoryAttribute("Version"), DefaultValueAttribute("1.0"), ReadOnlyAttribute(true)]
    public String AppVersion
    {
        get { return appVersion; }
        set { appVersion = value; }
    }
}
    /*
    public class BeaconPropTypeEditor : CollectionEditor
    {

        public BeaconPropTypeEditor(Type type) : base(type) { }

        public override Object EditValue(ITypeDescriptorContext context, IServiceProvider provider, Object value)
        {
            Object result = base.EditValue(context, provider, value);

            // assign the temporary collection from the UI to the property
            ((ClassContainingStuffProperty)context.Instance).Stuff = (List<BeaconProp>)result;

            return result;
        }
    }
*/



/*
public class Coordinates : List<double>
        {

        }
*/
/*
public class Room
{
    private Coordinate FCoord = new Coordinate();
    public Coordinate coordinate { get { return FCoord; } }
    public int minor;
    public int rssi;
    public String roomId;
    public String name;
    public int roomNumber;
}
*/

