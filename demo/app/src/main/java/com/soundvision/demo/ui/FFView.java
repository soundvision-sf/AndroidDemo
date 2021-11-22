package com.soundvision.demo.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.constraintlayout.solver.widgets.Rectangle;

import com.soundvision.demo.location.ffgeojson.FFGeoJson;
import com.soundvision.demo.location.ffgeojson.Feature;
import com.soundvision.demo.location.ffgeojson.Geometry;
import com.soundvision.demo.location.ffgeojson.GeometryLineString;
import com.soundvision.demo.location.ffgeojson.GeometryPoint;
import com.soundvision.demo.location.ffgeojson.GeometryPolygon;
import com.soundvision.demo.location.ffgeojson.IGeometry;
import com.soundvision.demo.location.flat.Floor;

import com.soundvision.demo.location.ffgeojson.PointD;
import com.soundvision.demo.location.flat.RouteNode;
import com.soundvision.demo.location.flat.RouteNodeLink;
import com.soundvision.demo.location.flat.VenueProp;
import com.soundvision.demo.location.flat.Zone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FFView extends View {


    public static int POINT_RADIUS = 12;

    public static int SEL_CODE_ROUTE_POINT = 3;
    public static int SEL_CODE_ROUTE_LINK = 4;

    public static int SEL_CODE_POI = 5;
    public static int SEL_CODE_BEACON = 7;

    private List<VenueProp> items = new ArrayList<VenueProp>();
    public VenueProp selArea = null;
    public Floor selFloor = null;

    private int width;
    private int height;


    //private Brush brush_newObject = new SolidBrush(Color.FromArgb(0x40ff0000));

    private PointD offset, mapOffset, drawOffset;
    private Double FScale = 0.2;
    private Rectangle rect = new Rectangle();

    private List<PointD> newObjectPath = null;
    private PointD GStart = new PointD();

    private PointD user = new PointD(5234, 1200);
    private float userDirection = 90.0f;
    private boolean showUser = true;
    private boolean validUserPos = false;
    private boolean lockUserCenter = false;

    private Paint linePen;
    private Paint dotBrush;
    private Paint polyBrush;
    private Paint polyPen;
    private Paint selectionPen;
    private Paint penGrid;
    private Paint fillBeacon;
    private Paint fillObstacle;
    private Paint fillUser1;
    private Paint fillUser2;
    private Paint fillUserWarn;

    private float polyPenWidth = 4.0f;


    public Map<String, Bitmap> icons = new HashMap<String, Bitmap>();
    public Bitmap iconBeacon ;
    public Bitmap iconPOI ;

    private List<Object> FSelection = new ArrayList<Object>();

    public FFView(Context context) {
        super(context);
        init();
    }

    public FFView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FFView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private Paint paintFill(int col)
    {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(col);
        return paint;
    }

    private Paint pen(int col, int width)
    {
        Paint paint = new Paint();
        paint.setStrokeWidth(width);
        paint.setPathEffect(null);
        paint.setColor(col);
        paint.setStyle(Paint.Style.STROKE);
        return paint;
    }

    public void centerUser(boolean lock)
    {
        lockUserCenter= lock;
        double x = (-(user.X) * FScale) + (width / 2);
        double y = (-(user.Y) * FScale) + (height / 2);
        mapOffset = new PointD(x, y);
        invalidate();;
    }

    private void initColors()
    {
        linePen = pen(0xffff0000, 12);
        dotBrush = paintFill(0xffff3636);
        polyBrush =  paintFill(0x16404040);// new SolidBrush(Color.FromArgb(20, 80, 80, 80));
        polyPen = pen(0xff808080, 5);
        selectionPen = pen(0xff004080, 3);
        penGrid = pen(0xff004080, 1);

        fillBeacon = paintFill(0x9f0030ff);

        Shader shader = new LinearGradient(0, 0, 10, 10, Color.LTGRAY, Color.GRAY, Shader.TileMode.REPEAT);
        Paint paint = new Paint();
        paint.setShader(shader);

        fillObstacle = paint;

        fillUser2 = paintFill(0xff000000);

        fillUser1 = paintFill(0xffa0a0a0);
        fillUserWarn = paintFill(0xffff4020);

    }

    private void init()
    {
        mapOffset = new PointD(0, 0);
        offset = new PointD(0, 0);
        initColors();
    }

    public PointD getOffset()
    {
        return mapOffset;
    }

    public void setOffset(PointD pt)
    {
        mapOffset = pt;
        lockUserCenter = false;
        invalidate();
    }

    public void setMapOffset(PointD pt)
    {
        offset = pt;
        invalidate();
    }

    public void setMapCoordOffset(PointD pt)
    {
        mapOffset = MapToDispScale(pt);
        invalidate();
    }

    public void setScale(double scale)
    {
        FScale = Math.max(0.1, Math.min(10.0, scale));
        invalidate();
    }

    public double getScale()
    {
        return FScale;
    }

    public void setUserLocation(PointD pt, boolean isValid)
    {
        user = pt;
        validUserPos = isValid;
        if (lockUserCenter) {
            centerUser(true);
        }
        else
        invalidate();
    }

    public void setUserLocation(PointD pt)
    {
        user = pt;
        invalidate();
    }

    public void setUserValidPos(boolean isValid)
    {
        validUserPos = isValid;
        invalidate();
    }

    public PointD getUserLocation()
    {
        return user;
    }

    public void setUserVisible(boolean v)
    {
        showUser = v;
        invalidate();
    }

    public boolean isUserVisible()
    {
        return showUser;
    }

    public void setUserDirection(float v)
    {
        userDirection = v;
        invalidate();
    }

    public float isUserDirection()
    {
        return userDirection;
    }


    public void Repaint()
    {}

    public void Clear()
    {
        FSelection.clear();
        items.clear();
        Repaint();
    }

    private Double DispToMapXCoord(Double x)
    {
        return (((x - mapOffset.X) / FScale) - offset.X);
    }

    private Double DispToMapYCoord(Double y)
    {
        return (((y - mapOffset.Y) / FScale) - offset.Y);
    }

    private PointD MapToDisp(PointD point)
    {
        return rotate_point(mapOffset.X + ((point.X + offset.X) * FScale), mapOffset.Y + ((point.Y + offset.Y) * FScale), 0.0);
    }

    private PointD MapToDispScale(PointD point)
    {
        return rotate_point(((point.X) * FScale),  ((point.Y) * FScale), 0.0);
    }

    private PointD dispToMapCoord(PointD pt)
    {
        return new PointD(DispToMapXCoord(pt.X), DispToMapYCoord(pt.Y));
    }

    private List<Double> createCoordinate(Double x, Double y)
    {

        ArrayList ret = new ArrayList<Double>();
        ret.add(DispToMapXCoord(x));
        ret.add(DispToMapYCoord(y));
        ret.add(0);
        return ret;
    }

    private Feature createFeature(int mode)
    {
        //String geometry = Geometry.Polygon;
        IGeometry geometry = null;
        Feature f = new Feature();
        f.type = "Feature";

        switch (mode)
        {
            case 1:
            {
                GeometryPolygon gp = new GeometryPolygon();
                List<List<List<Double>>> coords = new ArrayList<List<List<Double>>>();
                gp.coordinates = coords;
                int count = newObjectPath.size() - 1;
                if (count < 3) return null;
                coords.add(new ArrayList<List<Double>>());
                for (int i = 0; i < count; i++)
                {
                    coords.get(0).add(createCoordinate(newObjectPath.get(i).X, newObjectPath.get(i).Y));
                }
                geometry = gp;
                f.type = Feature.Zone;
            }
            break;
            case 2:
            {
                GeometryPolygon gp = new GeometryPolygon();
                List<List<List<Double>>> coords = new ArrayList<List<List<Double>>>();
                gp.coordinates = coords;
                coords.add(new ArrayList<List<Double>>());
                coords.get(0).add(createCoordinate(newObjectPath.get(0).X, newObjectPath.get(0).Y));
                coords.get(0).add(createCoordinate(newObjectPath.get(1).X, newObjectPath.get(0).Y));
                coords.get(0).add(createCoordinate(newObjectPath.get(1).X, newObjectPath.get(1).Y));
                coords.get(0).add(createCoordinate(newObjectPath.get(0).X, newObjectPath.get(1).Y));
                coords.get(0).add(createCoordinate(newObjectPath.get(0).X, newObjectPath.get(0).Y));
                geometry = gp;
                f.type = Feature.Zone;
            }
            break;
            case 4:
            {

                GeometryLineString gp = new GeometryLineString();
                List<List<Double>> coords = new ArrayList<List<Double>>();
                gp.coordinates = coords;


                int count = newObjectPath.size() - 1;
                if (count < 2) return null;
                //coords.add(new ArrayList<Double>());
                for (int i = 0; i < count; i++)
                {
                    coords.add(createCoordinate(newObjectPath.get(i).X, newObjectPath.get(i).Y));
                }
                geometry = gp;
            }
            break;
            case 5:
            {
                geometry = new GeometryPoint(createCoordinate(newObjectPath.get(0).X, newObjectPath.get(0).Y));
                f.type = Feature.POI;
            }
            break;
            case 6:
            {
                GeometryLineString gp = new GeometryLineString();
                List<List<Double>> coords = new ArrayList<List<Double>>();
                gp.coordinates = coords;
                int count = newObjectPath.size() - 1;
                if (count < 2) return null;
                for (int i = 0; i < count; i++)
                {
                    coords.add(createCoordinate(newObjectPath.get(i).X, newObjectPath.get(i).Y));
                }
                geometry = gp;
                f.type = Feature.Route;
            }
            break;
            case 7:
            {

                int count = newObjectPath.size() - 1;
                if (count < 0) return null;
                GeometryPoint gp = new GeometryPoint();
                //for (int i = 0; i < count; i++)
                {
                    gp.coordinates = createCoordinate(newObjectPath.get(newObjectPath.size() - 1).X, newObjectPath.get(newObjectPath.size() - 1).Y);
                }
                geometry = gp;
                f.type = Feature.Beacon;
            }
            break;
        }

        f.geometry = geometry;

        return f;
    }


    public void addGeoJson(FFGeoJson gs)
    {
        //items.Clear();
        //items.add(gs);

        offset = new PointD(-gs.bounds.X, -gs.bounds.Y);

        //offset = new PointD(-gs.BoundingBox.ToList()[0], -gs.BoundingBox.ToList()[1]);
        Repaint();
    }

    public void addArea(VenueProp area)
    {
        //items.Clear();
        if (items.indexOf(area)<0)
            items.add(area);

        selArea = area;

        for (Floor floor : area.floors)
        // if (floor.visible)
        {
                    /*
                    for (Zone zone : floor.zones)
                        if (zone.visible)
                        {
                            for (Feature f : zone.features)
                            {
                                f.type = Feature.Zone;
                            }
                        }
                    */
            for (Feature f : floor.beacons)
            {
                f.type = Feature.Beacon;
            }

            for (Feature f : floor.POI)
            {
                f.type = Feature.POI;
            }

        }

        Repaint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawOffset = new PointD(0, 0);

        canvas.drawLine(width / 2.0f, 0.0f, width / 2.0f, height, penGrid);
        canvas.drawLine(0, height / 2.0f, width, height / 2.0f, penGrid);

        for (VenueProp v : items)
        {
            DrawLayer(canvas, v, mapOffset, 0.0, FScale);
        }

        if (showUser && user != null) {
            DrawUserLocation(canvas, mapOffset, FScale, 0.0);
        }
    }

    private void DrawUserLocation(Canvas g, PointD ofs, double scale, double angle)
    {
        //PointD dmy = new PointD(0, 0);

        DrawSimplePoint(g, user, validUserPos ? fillUser1 : fillUserWarn, 36, ofs, angle, scale, -1);
        DrawSimplePoint(g, user, fillUser2, 22, ofs, angle, scale, -1);

        PointF pt = rotate_pointF(((user.X + offset.X) * scale), ((user.Y + offset.Y) * scale), angle);

        Path gp = new Path();

        gp.moveTo((float)ofs.X + pt.x, (float)ofs.Y + pt.y - 50);
        gp.lineTo((float)ofs.X + pt.x-30, (float)ofs.Y + pt.y - 40);
        gp.lineTo((float)ofs.X + pt.x, (float)ofs.Y + pt.y - 70);
        gp.lineTo((float)ofs.X + pt.x+30, (float)ofs.Y + pt.y - 40);
        gp.close();

        Matrix mMatrix = new Matrix();
        mMatrix.postRotate(userDirection, (float)ofs.X + pt.x, (float)ofs.Y + pt.y);
        gp.transform(mMatrix);

        g.drawPath(gp, fillUser2);

    }
    
    private void DrawPolygon(Canvas g, Feature feature, int zoneID, PointD offs, Double angle, Double scale, int selection)
    {
        if (!feature.visible) return;
        Path gp = new Path();
        PointF[] mainP = null;

        GeometryPolygon gpopy = (GeometryPolygon)feature.geometry;

        if (gpopy == null || gpopy.coordinates == null) return;

        for (List<List<Double>> coords : gpopy.coordinates)
        {
            PointF[] pt = new PointF[coords.size()];
            for (int i=0; i < coords.size(); i++)
            {
                pt[i] = rotate_pointF(offs.X + ((coords.get(i).get(0)  + offset.X) * scale), offs.Y + ((coords.get(i).get(1)  + offset.Y) * scale), angle);
                if (i == 0) {
                    gp.moveTo(pt[i].x, pt[i].y);
                } else {
                    gp.lineTo(pt[i].x, pt[i].y);
                }
            }
            gp.close();
            if (mainP == null) {
               // mainP = new PointF[pt.length];
                mainP = Arrays.copyOf(pt, pt.length);
               // pt.CopyTo(mainP, 0);
            }
          /*  if (selection >= 0 || FSelection.indexOf(coords) >= 0)
            {
                g.DrawPolygon(selectionPen, pt);
            }*/

        }

        if (feature.type.equals(Feature.Obstacle))
        {
            g.drawPath(gp, fillObstacle);
        }
        else
            g.drawPath(gp, polyBrush);

        g.drawPath(gp, polyPen);
       
    }

    private void DrawLinePoints(Canvas g, PointF[] pt, int zoneID, int codeID, PointD offs, Double angle, Double scale, int selection)
    {
        //float[] lines = new float[pt.length * 2];
        for (int i = 0; i < pt.length; i++)
        {
            pt[i] = rotate_pointF(offs.X + ((pt[i].x + offset.X) * scale), offs.Y + ((pt[i].y + offset.Y) * scale), angle);
           // lines[i * 2] = pt[i].x;
           // lines[1+(i * 2)] = pt[i].y;
        }
/*
        if (selection >= 0)
        {
            g.DrawLines(selectionPen, pt);

        }
*/
        //g.drawLine(0, 0, 678, 678, linePen);
        for (int i = 0; i < pt.length-1; i++)
        {
            g.drawLine(pt[i].x ,pt[i].y, pt[i+1].x, pt[i+1].y, linePen);
        }
      //  g.drawLines(lines, linePen);
      
    }

    private void DrawLine(Canvas g, Feature feature, int zoneID, int codeID, PointD offs, Double angle, Double scale, int selection)
    {
        if (!feature.visible) return;

        GeometryLineString gline = (GeometryLineString)feature.geometry;
        PointF[] pt = new PointF[gline.coordinates.size()];
        for (int i = 0; i < gline.coordinates.size(); i++)
        {
            pt[i] = new PointF((gline.coordinates.get(i).get(0) ).floatValue(), (gline.coordinates.get(i).get(1) ).floatValue());
        }
        DrawLinePoints(g, pt, zoneID, codeID, offs, angle, scale, selection);
    }

    private void DrawSimplePoint(Canvas g, PointD point, Paint paint, int rad, PointD offs, Double angle, Double scale, int selection)
    {
        PointD pt = rotate_point(offs.X + ((point.X + offset.X) * scale), offs.Y + ((point.Y + offset.Y) * scale), angle);
        RectF rc = new RectF((float)pt.X - rad, (float)pt.Y - rad, (float)pt.X +rad, (float)pt.Y +rad);

        g.drawOval(rc, paint) ;
        
    }

    private void DrawImage(Canvas g, Bitmap image, PointD point, int zoneID, int codeID, PointD offs, Double angle, Double scale, int selection)
    {
        PointD pt = rotate_point(offs.X + ((point.X + offset.X) * scale), offs.Y + ((point.Y + offset.Y) * scale), angle);

        // 19 pixels offset
        PointF imgPt = new PointF((float)pt.X - (image.getWidth() / 2), (float)pt.Y - (image.getHeight() - 40));
        if (selection >= 0)
        {
            Paint paint = new Paint();
            ColorFilter filter = new PorterDuffColorFilter(0xffff0000, PorterDuff.Mode.SRC_IN);
            paint.setColorFilter(filter);
            g.drawBitmap(image, imgPt.x, imgPt.y, null);
        } else
        {
            g.drawBitmap(image, imgPt.x, imgPt.y, null);
        }
        //
    }

    private void DrawPoint(Canvas g, Feature feature, Paint paint, int rad, PointD offs, Double angle, Double scale, int selection)
    {
        if (!feature.visible) return;
        GeometryPoint gp = (GeometryPoint)feature.geometry;
        //for (List<List<Double>> coords : feature.geometry.coordinates)
        {
            //for (int i = 0; i < coords.Count; i++)
            {
                DrawSimplePoint(g, new PointD(gp.coordinates.get(0), gp.coordinates.get(1)), paint, rad, offs, angle, scale, FSelection.indexOf(feature));
            }
        }
    }

    private void DrawLayer(Canvas g, VenueProp gs, PointD offs, Double angle, Double scale)
    {
        int zoneID = 0;

        if (gs.desc != null)
            DrawPolygon(g, gs.desc, 255, offs, angle, scale, FSelection.indexOf(gs.desc));

        for (Floor floor : gs.floors)
        if (floor.visible)
        {
            for (Zone zone : floor.zones)
            if (zone.visible)
            {
                for (Feature f : zone.features)
                {
                    if (f.geometry.type().equals(Geometry.Polygon)) DrawPolygon(g, f, zoneID, offs, angle, scale, FSelection.indexOf(f));
                    else
                    if (f.geometry.type().equals(Geometry.LineString)) DrawLine(g, f, zoneID, 0, offs, angle, scale, FSelection.indexOf(f));
                    else
                    if (f.geometry.type().equals(Geometry.Point)) DrawPoint(g, f, dotBrush, POINT_RADIUS, offs, angle, scale, FSelection.indexOf(f));
                }
                zoneID++;
            }




            zoneID = 0;
            if (floor.routeCtrl.visible)
                for (RouteNodeLink link : floor.routeLinks)
            {

                PointF[] pt = new PointF[2];

                pt[0] = floor.routeCtrl.GetRoutePt(link.a).toPointF();
                pt[1] = floor.routeCtrl.GetRoutePt(link.b).toPointF();

                DrawLinePoints(g, pt, zoneID, SEL_CODE_ROUTE_LINK, offs, angle, scale, FSelection.indexOf(link));
                zoneID++;
            }

            zoneID = 0;
            if (floor.routeCtrl.visible)
            for (RouteNode routePt : floor.routeNodes)
            {
                DrawSimplePoint(g, routePt.coordinate, dotBrush, 12, offs, angle, scale, FSelection.indexOf(routePt));
                zoneID++;
            }


            zoneID = 0;
            for (Feature f : floor.POI)
            {
                if (f.visible)
                {
                    if (iconPOI != null && f.geometry.type().equals(Geometry.Point))
                    {
                        GeometryPoint gp = (GeometryPoint)f.geometry;
                        //CtrlPOI cp = new CtrlPOI(f);
                        String category = f.property("category");
                        if (icons.containsKey(category))
                        {
                            DrawImage(g, icons.get(category), new PointD(gp.coordinates.get(0) , gp.coordinates.get(1) ), zoneID, SEL_CODE_POI, offs, angle, scale, FSelection.indexOf(f));
                        }
                        else
                            DrawImage(g, iconPOI, new PointD(gp.coordinates.get(0) , gp.coordinates.get(1) ), zoneID, SEL_CODE_POI, offs, angle, scale, FSelection.indexOf(f));
                    }
                    //DrawPoint(g, f, zoneID, SEL_CODE_POINT, offs, angle, scale, FSelection.indexOf(f));
                }
                zoneID++;
            }

            zoneID = 0;
            for (Feature f : floor.beacons)
            {
                if (f.visible)
                {
                    if (iconBeacon != null && f.geometry.type().equals(Geometry.Point))
                    {
                        //GeometryPoint gp = (GeometryPoint)f.geometry;

                        DrawPoint(g, f, fillBeacon, POINT_RADIUS, offs, angle, scale, FSelection.indexOf(f));
                        //DrawImage(g, iconBeacon, new PointD(gp.coordinates.get(0) + f.offsetX, gp.coordinates.get(1) + f.offsetY), zoneID, SEL_CODE_BEACON, offs, angle, scale, FSelection.indexOf(f));
                    }
                    //DrawPoint(g, f, zoneID, SEL_CODE_POINT, offs, angle, scale, FSelection.indexOf(f));
                }
                zoneID++;
            }

        }
    }
/*
    private void DrawGrid(Canvas g, Double scale)
    {
        float w = 100.0f * (float)scale;
        if (w > 5)
        {
            float x = (float)mapOffset.X % w;

            while (x < Width)
            {
                g.DrawLine(penGrid, x, 0, x, Height);
                x += w;
            }
            x = (float)mapOffset.Y % w;
            while (x < Width)
            {
                g.DrawLine(penGrid, 0, x, Width, x);
                x += w;
            }
        }
        g.DrawLine(Pens.LightGray, (float)mapOffset.X , 0, (float)mapOffset.X , Height);
        g.DrawLine(Pens.LightGray, 0, (float)mapOffset.Y, Width, (float)mapOffset.Y );

    }
*/
    /*
    private void drawLabel(Canvas g, PointD p1, PointD p2, String text)
    {
        if (Math.Abs(p1.X-p2.X) > Math.Abs(p1.Y - p2.Y))
        {
            Double y = Math.Min(p1.Y, p2.Y) - 27;
            StringFormat sf = new StringFormat();
            sf.LineAlignment = StringAlignment.Center;
            sf.Alignment = StringAlignment.Center;
            RectangleF r = new RectangleF((float)p1.X-100, (float)y, (float)Math.Abs((float)p2.X-p1.X)+200, 27);
            g.DrawString(text, this.Font, Brushes.Black, r, sf);
        } else
        {

            Double x = Math.Min(p1.X, p2.X) - 27;
            StringFormat sf = new StringFormat();
            sf.LineAlignment = StringAlignment.Center;
            sf.Alignment = StringAlignment.Far;
            RectangleF r = new RectangleF((float)x -200, (float)p1.Y-50, 200, (float)Math.Abs(p2.Y-p1.Y)+100);
            g.DrawString(text, this.Font, Brushes.Black, r, sf);
        }
    }
*/
    /*
    private void Order(ref PointD p1, ref PointD p2)
    {
        Double x1 = Math.Min(p1.X, p2.X);
        Double x2 = Math.Max(p1.X, p2.X);
        Double y1 = Math.Min(p1.Y, p2.Y);
        Double y2 = Math.Max(p1.Y, p2.Y);
        p1.X = x1;
        p2.X = x2;
        p1.Y = y1;
        p2.Y = y2;
    }
*/
    private PointD rotate_point(PointD p, Double angle)
    {
        if (angle == 0.0) return p;
        return new PointD(drawOffset.X + (Double)(Math.cos(angle) * (p.X) - Math.sin(angle) * (p.Y)),
                (drawOffset.Y + (Double)(Math.sin(angle) * (p.X) + Math.cos(angle) * (p.Y))));
    }

    private PointD rotate_point(Double pX, Double pY, Double angle)
    {
        if (angle == 0.0) return new PointD(drawOffset.X + pX, drawOffset.Y + pY);
        return new PointD((float)(drawOffset.X + (Math.cos(angle) * (pX) - Math.sin(angle) * (pY))),
                ( (float)(drawOffset.Y + (Math.sin(angle) * (pX) + Math.cos(angle) * (pY)))));
    }

    private PointF rotate_pointF(Double pX, Double pY, Double angle)
    {
        if (angle == 0.0) return new PointF((float)drawOffset.X + pX.floatValue(), (float)drawOffset.Y + pY.floatValue());
        return new PointF((float)(drawOffset.X + (Math.cos(angle) * (pX) - Math.sin(angle) * (pY))),
                ((float)(drawOffset.Y + (Math.sin(angle) * (pX) + Math.cos(angle) * (pY)))));
    }

    public void drawToCanvas(Canvas g, Double x, Double y, Double angle, Double scale)
    {
        drawOffset = new PointD(x, y);
        PointD dmy = new PointD(0, 0);
        for (VenueProp v : items)
        {
            DrawLayer(g, v, dmy, angle, scale);
        }
        //DrawNewObject(g);
    }

    public void AddSelection(Object f)
    {
        if (FSelection.indexOf(f) < 0)
            FSelection.add(f);
    }

    public void ClearSelection()
    {
        FSelection.clear();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
        mapOffset = new PointD(width / 2, height / 2);
    }

}
