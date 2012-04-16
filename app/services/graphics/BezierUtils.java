/*
  Copyright (c) 2006 Adrian Colomitchi

  Permission is hereby granted, free of charge, to any person
  obtaining a copy of this software and associated documentation
  files (the "Software"), to deal in the Software without 
  restriction, including without limitation the rights to use, 
  copy, modify, merge, publish, distribute, sublicense, and/or 
  sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following 
  conditions:

  The above copyright notice and this permission notice 
  shall be included in all copies or substantial portions
  of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
  ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
  TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
  PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
  SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
  ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  THE SOFTWARE.
 */

package services.graphics;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.QuadCurve2D;

/**
 * Utility functions for processing B&eacute;zier curves. Copied from original com.caffeineowl.graphics.
 * @author Adrian Colomitchi (acolomitchi(monkey_tail)gmail.com).
 *
 * A bunch of cool functionality that isn't totally helpful at Egraphs was removed.
 */
class BezierUtils {
  /**
   * Subdivides a cubic B&eacute;zier at a given value for the curve's parameter.
   * The method will return the resulted cubic segments
   * in the two output parameters (<code>first</code> and <code>second</code>).
   * It is safe for whichever of them to be the same as the provided cubic,
   * the computation will be correct, however the original cubic will be
   * overwritten. The method will not check if the both of the
   * result cubic segments point to the same instance:
   * in this case the last to be computed (the <code>second</code>) 
   * will overwrite the previous.<br>
   * The method uses de Casteljau algorithm.
   * @param cubic the cubic B&eacute;zier to split
   * @param tSplit the value for parameter where the split should occur.
   *               If out of the 0..1 range, the function does nothing
   *               and returns <code>false</code>.
   * @param first the place where the first cubic segment that results
   *              upon subdivision is stored. If <code>null</code>,
   *              the method will interpret this as "don't care about first
   *              segment" and will not compute it.
   * @param second the place where the second cubic segment that results
   *              upon subdivision is stored. If <code>null</code>,
   *              the method will interpret this as "don't care about second
   *              segment" and will not compute it.
   * @return <code>true</code> if the computation was performed and finished
   *   successfully, <code>false</code> otherwise (i.e a <code>null</code> value
   *   for the <code>cubic</code> parameter, or a value for the <code>tSplit</code>
   *   out of the <code>[0..1]</code> range).
   */
  static public boolean splitCurve(CubicCurve2D cubic, double tSplit, CubicCurve2D first, CubicCurve2D second) {
    boolean toRet=tSplit>=0 && tSplit<=1 && null!=cubic;
    if(toRet && ((null!=first) || (null!=second))) {
      double x0=cubic.getX1();
      double x1=cubic.getX2();
      double cx0=cubic.getCtrlX1();
      double cx1=cubic.getCtrlX2();
      double y0=cubic.getY1();
      double y1=cubic.getY2();
      double cy0=cubic.getCtrlY1();
      double cy1=cubic.getCtrlY2();

      double p0x=x0+(tSplit*(cx0-x0));
      double p0y=y0+(tSplit*(cy0-y0));
      double p1x=cx0+(tSplit*(cx1-cx0));
      double p1y=cy0+(tSplit*(cy1-cy0));
      double p2x=cx1+(tSplit*(x1-cx1));
      double p2y=cy1+(tSplit*(y1-cy1));

      double p01x=p0x+(tSplit*(p1x-p0x));
      double p01y=p0y+(tSplit*(p1y-p0y));
      double p12x=p1x+(tSplit*(p2x-p1x));
      double p12y=p1y+(tSplit*(p2y-p1y));

      double dpx=p01x+(tSplit*(p12x-p01x));
      double dpy=p01y+(tSplit*(p12y-p01y));

      if(null!=first) {
        first.setCurve(x0, y0, p0x, p0y, p01x, p01y, dpx, dpy);
      }

      if(null!=second) {
        second.setCurve(dpx, dpy, p12x, p12y, p2x, p2y, x1, y1);
      }
    }
    return toRet;
  }
  
  
  
  /**
   * Subdivides a quadratic B&eacute;zier at a given value for the curve's parameter.
   * The method will return the resulted quadratic B&eacute;zier segments
   * in the two output parameters (<code>first</code> and <code>second</code>).
   * It is safe for whichever of them to be the same as the provided quadratic,
   * the computation will be correct, however the original quadratic will be
   * overwritten. The method will not check if the both of the
   * result quadratic curves point to the same instance:
   * in this case the last to be computed (the <code>second</code>) 
   * will overwrite the previous.<br>
   * The method uses de Casteljau algorithm.
   * @param quad the cubic B&eacute;zier to split
   * @param tSplit the value for parameter where the split should occur.
   *               If out of the 0..1 range, the function does nothing
   *               and returns <code>false</code>.
   * @param first the place where the first quadratic segment that results
   *              upon subdivision is placed. If <code>null</code>,
   *              the method will interpret this as "don't care about first
   *              segment" and will not compute it.
   * @param second the place where the second quadratic segment that results
   *              upon subdivision is placed. If <code>null</code>,
   *              the method will interpret this as "don't care about second
   *              segment" and will not compute it.
   * @return <code>true</code> if the computation was performed and finished
   *   successfully, <code>false</code> otherwise (i.e a <code>null</code> value
   *   for the <code>quad</code> parameter, or a value for the <code>tSplit</code>
   *   out of the <code>[0..1]</code> range).
   */
  static public boolean splitCurve(QuadCurve2D quad, double tSplit, QuadCurve2D first, QuadCurve2D second) {
    boolean toRet=tSplit>=0 && tSplit<=1 && null!=quad;
    if(toRet && ((null!=first) || (null!=second))) {
      double sx0=quad.getX1();
      double sx1=quad.getX2();
      double scx=quad.getCtrlX();
      double scy=quad.getCtrlY();
      double sy0=quad.getY1();
      double sy1=quad.getY2();

      double p0x=sx0+(tSplit*(scx-sx0));
      double p0y=sy0+(tSplit*(scy-sy0));
      double p1x=scx+(tSplit*(sx1-scx));
      double p1y=scy+(tSplit*(sy1-scy));

      double dpx=p0x+(tSplit*(p1x-p0x));
      double dpy=p0y+(tSplit*(p1y-p0y));

      if(null!=first) {
        first.setCurve(sx0, sy0, p0x, p0y, dpx, dpy);
      }

      if(null!=second) {
        second.setCurve(dpx, dpy, p1x, p1y, sx1, sy1);
      }
    }
    return toRet;
  }
}
