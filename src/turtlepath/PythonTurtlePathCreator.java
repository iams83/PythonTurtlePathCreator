package turtlepath;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;

public class PythonTurtlePathCreator
{
    static public void main(String[] args) throws IOException
    {
        Font font = new Font("Arial Unicode MS", Font.PLAIN, 64);
        
        String text = "\u265E Check mate!";
        
        File pythonOutputFile = null;                       // Set to null to write python code into stdout
        
        File outputImageFile = new File("C:\\tmp\\1.png");  // Set to null to write python code into stdout
        
        writePythonCodeAndDrawOntoImage(font, text, pythonOutputFile, outputImageFile);
    }

    static void writePythonCodeAndDrawOntoImage(Font font, String text, 
            File pythonOutputFile, File outputImageFile) throws FileNotFoundException
    {
        BufferedImage image = new BufferedImage(800, 100, BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g = (Graphics2D) image.getGraphics();
        
        TextLayout textLayout = new TextLayout(text, font, g.getFontRenderContext());
        
        Shape shape = textLayout.getOutline(new AffineTransform());
        
        ArrayList<TurtlePath> turtlePaths = PythonTurtlePathCreator.createTurtlePath(shape);
        
        if (outputImageFile != null)
        {
            g.setColor(Color.blue);
            
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            
            g.translate(image.getWidth() / 2, (image.getHeight() + g.getFontMetrics(font).getAscent()) / 2);
            
            for (TurtlePath p : turtlePaths)
            {
                if (p.isHole())
                    g.setColor(Color.blue);
                else
                    g.setColor(Color.cyan);
                
                g.fill(p.getPath2D());
            }
            
            g.setColor(Color.black);
            
            for (TurtlePath p : turtlePaths)
            {
                g.draw(p.getPath2D());
            }
            
            try
            {
                ImageIO.write(image, "png", outputImageFile);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        try (PrintStream out = pythonOutputFile == null ? System.out : 
            new PrintStream(new FileOutputStream(pythonOutputFile)))
        {
            PythonTurtlePathCreator.writePython(out, turtlePaths, "red");
        }
    }

    static class TurtlePos
    {
        final double angle, x, y;
        
        public TurtlePos(double angle, double x, double y)
        {
            this.angle = angle;
            this.x = x;
            this.y = y;
        }
    }
    
    static class TurtlePath
    {
        final private Point2D start;
    
        final private ArrayList<Point2D> movements = new ArrayList<>();
        
        private boolean isHole = false;
        
        public TurtlePath(double startX, double startY)
        {
            this.start = new Point2D.Double(startX, startY);
        }
        
        private Point2D getLastPoint()
        {
            if (this.movements.isEmpty())
                return this.start;
            else
                return this.movements.get(this.movements.size() - 1);
        }

        public void addCubicTo(double x2, double y2, double tx1, double ty1, double tx2, double ty2)
        {
            Point2D lastPoint = getLastPoint();
            
            double x1 = lastPoint.getX();
            double y1 = lastPoint.getY();
            
            ArrayList<Point2D> tentativePath = getCubicPath(x1, y1, x2, y2, tx1, ty1, tx2, ty2, 10);
            
            tentativePath.add(0, lastPoint);
            
            double pathLength = getPathLength(tentativePath);
            
            this.movements.addAll(getCubicPath(x1, y1, x2, y2, tx1, ty1, tx2, ty2, (int) (pathLength / 3)));
        }
    
        public void addQuadTo(double x2, double y2, double tx, double ty)
        {
            Point2D lastPoint = getLastPoint();
            
            double x1 = lastPoint.getX();
            double y1 = lastPoint.getY();
            
            ArrayList<Point2D> tentativePath = getQuadPath(x1, y1, x2, y2, tx, ty, 10);
            
            tentativePath.add(0, lastPoint);
            
            double pathLength = getPathLength(tentativePath);
            
            this.movements.addAll(getQuadPath(x1, y1, x2, y2, tx, ty, (int) (pathLength / 3)));
        }


        private ArrayList<Point2D> getCubicPath(double x1, double y1, double x2, double y2, 
                double tx1, double ty1, double tx2, double ty2, int chunks)
        {
            ArrayList<Point2D> points = new ArrayList<Point2D>();
            
            for (int i = 1; i <= chunks; i ++)
            {
                double d = 1. * i / chunks;
                
                double px1 = x1 + (tx1 - x1) * d;
                double py1 = y1 + (ty1 - y1) * d;

                double px2 = tx1 + (tx2 - tx1) * d;
                double py2 = ty1 + (ty2 - ty1) * d;
                
                double px3 = tx2 + (x2 - tx2) * d;
                double py3 = ty2 + (y2 - ty2) * d;
                
                double qx1 = px1 + (px2 - px1) * d;
                double qy1 = py1 + (py2 - py1) * d;

                double qx2 = px2 + (px3 - px2) * d;
                double qy2 = py2 + (py3 - py2) * d;
                
                points.add(new Point2D.Double(
                        qx1 + (qx2 - qx1) * d, 
                        qy1 + (qy2 - qy1) * d));
            }
                
            return points;
        }
        
        private ArrayList<Point2D> getQuadPath(double x1, double y1, double x2,
                double y2, double tx, double ty, int chunks)
        {
            ArrayList<Point2D> points = new ArrayList<>();
            
            for (int i = 1; i <= chunks; i ++)
            {
                double d = 1. * i / chunks;
                
                double px1 = x1 + (tx - x1) * d;
                double py1 = y1 + (ty - y1) * d;

                double px2 = tx + (x2 - tx) * d;
                double py2 = ty + (y2 - ty) * d;
                
                points.add(new Point2D.Double(
                        px1 + (px2 - px1) * d, 
                        py1 + (py2 - py1) * d));
            }
            return points;
        }

        private double getPathLength(ArrayList<Point2D> path)
        {
            double d = 0;
            
            for (int i = path.size() - 1, j = 0; j < path.size(); i = j ++)
                d += path.get(i).distance(path.get(j));
            
            return d;
        }

        public void addLineTo(double x, double y)
        {
            this.movements.add(new Point2D.Double(x, y));
        }
    
        private void setHole()
        {
            this.isHole = true;
        }
        
        public boolean isHole()
        {
            return this.isHole;
        }
    
        public Path2D getPath2D()
        {
            Path2D.Double path = new Path2D.Double();
            
            path.moveTo(this.start.getX(), this.start.getY());
            
            for (Point2D p : this.movements)
                path.lineTo(p.getX(), p.getY());
            
            path.closePath();
            
            return path;
        }
    
        private TurtlePos lineTo(ArrayList<String> output, TurtlePos pos, double x2, double y2)
        {
            double hypot = Math.hypot(x2 - pos.x, y2 - pos.y);
            double newAngle = Math.atan2(y2 - pos.y, x2 - pos.x);
            
            double newAngleDeg = newAngle * 180 / Math.PI;
            double angleDeg = pos.angle * 180 / Math.PI;
            
            double d = Math.abs(newAngleDeg - angleDeg) % 360; 
            double nearestAngle = d > 180 ? 360 - d : d;
    
            nearestAngle *= (newAngleDeg - angleDeg >= 0   && newAngleDeg - angleDeg <= 180) || 
                            (newAngleDeg - angleDeg <=-180 && newAngleDeg - angleDeg>= -360) ? 1 : -1;
            
            if (nearestAngle > 0)
                output.add("turtle.right(" + nearestAngle + ")");
            else
                output.add("turtle.left(" + -nearestAngle + ")");
            
            output.add("turtle.forward(" + hypot + ")");
            
            return new TurtlePos(newAngle, x2, y2);
        }
    
        public TurtlePos writeTurtlePath(ArrayList<String> output, TurtlePos pos)
        {
            if (!this.movements.isEmpty())
            {
                pos = this.lineTo(output, pos, 
                        this.start.getX(), 
                        this.start.getY());
        
                output.add("turtle.pendown()");
                output.add("turtle.begin_fill()");
                
                for (int i = 0; i < this.movements.size(); i ++)
                    pos = this.lineTo(output, pos, 
                            this.movements.get(i).getX(), 
                            this.movements.get(i).getY());
        
                pos = this.lineTo(output, pos, 
                        this.start.getX(), 
                        this.start.getY());
        
                output.add("turtle.end_fill()");
                output.add("turtle.penup()");
            }
                
            return pos;
        }
    }

    static ArrayList<TurtlePath> createTurtlePath(Shape shape)
    {
        double jmpX = 0, jmpY = 0;
        
        boolean jump = false;
        
        AffineTransform at = new AffineTransform();
        at.translate(- shape.getBounds().getWidth() / 2, 0);
        
        ArrayList<TurtlePath> turtlePaths = new ArrayList<TurtlePath>();
        
        TurtlePath main = new TurtlePath(0, 0);
        
        turtlePaths.add(main);
        
        for (PathIterator iterator = shape.getPathIterator(at); !iterator.isDone(); iterator.next())
        {
            double coords[] = new double[4];
            
            int type = iterator.currentSegment(coords);
            
            switch(type)
            {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_CLOSE:
                {
                    jmpX = coords[0];
                    jmpY = coords[1];
                    jump = true;

                    break;
                }

                case PathIterator.SEG_CUBICTO:
                {
                    if (jump)
                    {
                        main = new TurtlePath(jmpX, jmpY);

                        turtlePaths.add(main);
                        
                        jump = false;
                    }
                    
                    main.addCubicTo(coords[4], coords[5], coords[0], coords[1], coords[2], coords[3]);
                    break;
                }
                
                case PathIterator.SEG_QUADTO:
                {
                    if (jump)
                    {
                        main = new TurtlePath(jmpX, jmpY);

                        turtlePaths.add(main);
                        
                        jump = false;
                    }
                    
                    main.addQuadTo(coords[2], coords[3], coords[0], coords[1]);
                    break;
                }
                
                case PathIterator.SEG_LINETO:
                {
                    if (jump)
                    {
                        main = new TurtlePath(jmpX, jmpY);

                        turtlePaths.add(main);
                        
                        jump = false;
                    }
                    
                    main.addLineTo(coords[0], coords[1]);
                    break;
                }
            }
        }

        sortTurtlePaths(turtlePaths);
        
        return turtlePaths;
    }
    
    static void sortTurtlePaths(ArrayList<TurtlePath> turtlePaths)
    {
        for (int j = 0; j < turtlePaths.size(); j ++)
        {
            TurtlePath turtlePathj = turtlePaths.get(j);
            
            for (int i = j + 1; i < turtlePaths.size(); i ++)
            {
                TurtlePath turtlePathi = turtlePaths.get(i);
                
                Path2D pathi = turtlePathi.getPath2D();
                
                if (pathi.contains(turtlePathj.start))
                {
                    turtlePathj.setHole();
                    
                    Collections.swap(turtlePaths, i, j);
                    
                    sortTurtlePaths(turtlePaths);
                    
                    return;
                }
            }
        }

        for (int j = 0; j < turtlePaths.size(); j ++)
        {
            TurtlePath turtlePathj = turtlePaths.get(j);
            
            for (int i = 0; i < turtlePaths.size(); i ++)
            {
                if (i == j)
                    continue;
                
                TurtlePath turtlePathi = turtlePaths.get(i);
                
                Path2D pathi = turtlePathi.getPath2D();
                
                if (pathi.contains(turtlePathj.start))
                {
                    turtlePathj.setHole();
                }
            }
        }
    }

    public static void writePython(PrintStream out, ArrayList<TurtlePath> turtleShapes, String fillingColor)
    {
        out.println("import turtle");
        out.println("turtle = turtle.Turtle()");
        
        out.println("turtle.penup()");
        
        TurtlePos pos = new TurtlePos(0, 0, 0);
        
        for (TurtlePath turtlePath : turtleShapes)
        {
            ArrayList<String> output = new ArrayList<>();
            
            if (turtlePath.isHole())
                out.println("turtle.fillcolor(\"white\")");
            else
                out.println("turtle.fillcolor(\"" + fillingColor + "\")");
            
            pos = turtlePath.writeTurtlePath(output, pos);

            out.println(String.join("\n", output) + "\n");
        }
        
        out.println("turtle.hideturtle()"); 
        out.println("turtle.done()");
    }
}
