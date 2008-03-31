//if[FLOATS]
/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.squawk.util;


/* This is the new Math Library in Java
 * mostly copied from the Sun Math library in C
 * follows ieee standards
 * Ben Adida(ben.adida@east.sun.com)
 * PERMANENT email address : ben@mit.edu
 * started May 28th, 1996
 * Christine H. Flood
 * started October 1, 1996
 *
 * arc trig functions extracted February 24, 2007 by Ron Goldman
 */


/**
 * The class <code>MathUtils</code> contains some of the Java SE Math routines that are not present in the CLDC 1.1 version of {@link java.lang.Math}: <p>
 * {@link MathUtils#asin}, {@link MathUtils#acos}, {@link MathUtils#atan} & {@link MathUtils#atan2}.
 *<p>
 *
 * The methods in this class are directly substitutable for the corresponding methods in Java SE java.lang.Math.
 *
 * @see <a href="http://java.sun.com/j2se/1.3/docs/api/java/lang/Math.html">java.lang.Math in Java SE</a> 
 * @see  java.lang.Math CLDC 1.1's  java.lang.Math
 */
public class MathUtils {
    
    private MathUtils() {
    }
    
    private static final long no_sign_mask = 0x7FFFFFFFFFFFFFFFL;
    private static final long exp_mask = 0x7FF0000000000000L;
    private static final long high_bits_mask = 0xffffffff00000000L;
    private static final long one = 0x3ff0000000000000L;
    private static final long half = 0x3fe0000000000000L;
    
    /* 0x3FF921FB, 0x54442D18 */
    private static final double pio2_hi =  1.57079632679489655800e+00;
    
    /* 0x3C900000, 0x00000000 */
    private static final double pio2_lo =  6.12323399573676603587e-17;
    
    /* 0x3FE921FB, 0x54442D18 */
    private static final double pio4_hi =  7.85398163397448278999e-01;
    
    /* 0x3C81A626, 0x33145C07 */
    private static final double pio4_lo =  3.06161699786838301793e-17;
    
    private static final double
            arc_pS0 =  1.66666666666666657415e-01, /* 0x3FC55555, 0x55555555 */
            arc_pS1 = -3.25565818622400915405e-01, /* 0xBFD4D612, 0x03EB6F7D */
            arc_pS2 =  2.01212532134862925881e-01, /* 0x3FC9C155, 0x0E884455 */
            arc_pS3 = -4.00555345006794114027e-02, /* 0xBFA48228, 0xB5688F3B */
            arc_pS4 =  7.91534994289814532176e-04, /* 0x3F49EFE0, 0x7501B288 */
            arc_pS5 =  3.47933107596021167570e-05, /* 0x3F023DE1, 0x0DFDF709 */
            arc_qS1 = -2.40339491173441421878e+00, /* 0xC0033A27, 0x1C8A2D4B */
            arc_qS2 =  2.02094576023350569471e+00, /* 0x40002AE5, 0x9C598AC8 */
            arc_qS3 = -6.88283971605453293030e-01, /* 0xBFE6066C, 0x1B8D0159 */
            arc_qS4 =  7.70381505559019352791e-02; /* 0x3FB3B8C5, 0xB12E9282 */
    
    
    /**
     * Returns the arc sine of an angle, in the range of -<i>pi</i>/2 through <i>pi</i>/2. 
     * Special cases:
     * <ul>
     *  <li>If the argument is NaN or its absolute value is greater than 1, then the result is NaN.
     *  <li>If the argument is zero, then the result is a zero with the same sign as the argument.
     * </ul>
     *
     * @param a the value whose arc sine is to be returned.
     * @return the arc sine of the argument.
     */
    public static double asin(double a) {
        double t, w, p, q, c, r, s;
        long hx = Double.doubleToLongBits(a);
        long ix = hx & no_sign_mask;
        
        if (Math.abs(a) >= 1) {
            if (Math.abs(a) == 1)  return(a * pio2_hi + a * pio2_lo);
            if (Math.abs(a) > 1) return(Double.NaN);
        } else if (ix < half) {     /* |x| < 0.5 */
            /* |x| < 2^-27 */
            if (ix < 0x3e40000000000000L) {
                return(a);
            } else {
                t = a * a;
                p = t * (arc_pS0 + t * (arc_pS1 + t * (arc_pS2 + t * (arc_pS3 + t * (arc_pS4 + t * arc_pS5)))));
                q = 1.0 + t * (arc_qS1 + t * (arc_qS2 + t * (arc_qS3 + t * arc_qS4)));
                w = p / q;
                return(a + a * w);
            }
        }
        
        /* 1 > |x| > 0.5 */
        w = 1.0 - Math.abs(a);
        t = w * 0.5;
        p = t * (arc_pS0 + t * (arc_pS1 + t * (arc_pS2 + t * (arc_pS3 + t * (arc_pS4 + t * arc_pS5)))));
        q = 1.0 + t * (arc_qS1 + t * (arc_qS2 + t * (arc_qS3 + t * arc_qS4)));
        s = Math.sqrt(t);

        /* if |x| >0.975 */
        if (ix >= 0x3fef333300000000L) {
            w = p / q;
            t = pio2_hi - (2.0 * (s + s * w) - pio2_lo);
        } else {
            w = s;
            w = Double.longBitsToDouble(Double.doubleToLongBits(w) & high_bits_mask);
            c = (t - w * w) / (s + w);
            r = p / q;
            p = 2.0 * s * r - (pio2_lo - 2.0 * c);
            q = pio4_hi - 2.0 * w;
            t = pio4_hi - (p - q);
        }
        
        if (hx > 0) return(t);
        else return(-t);
    }
    
    /**
     * Returns the arc cosine of an angle, in the range of 0 through <i>pi</i>. 
     * Special cases:
     * <ul>
     *  <li>If the argument is NaN or its absolute value is greater than 1, then the result is NaN.
     * </ul>
     *
     * @param a the value whose arc cosine is to be returned.
     * @return the arc cosine of the argument.
     */
    public static double acos(double a) {
        double z, p, q, r, w, s, c, df;
        long hx = Double.doubleToLongBits(a);
        long ix = hx & no_sign_mask;
        
        /* |x| >= 1 */
        if (Math.abs(a)  >= 1) {
            /* |x| == 1*/
            if (ix == one) {
                if (hx > 0) return(0.0);
                else return(Math.PI + 2.0 * pio2_lo);
            }
            /* acos(|x| > 1) is NaN */
            return((a - a) / (a - a));
        }
        
        /* |x| < 0.5 */
        if (ix < half) {
            /* if |x| < 2^-57 */
            if (ix <= 0x3c600000ffffffffL) return(pio2_hi + pio2_lo);
            z = a * a;
            p = z * (arc_pS0 + z * (arc_pS1 + z * (arc_pS2 + z * (arc_pS3 + z * (arc_pS4 + z * arc_pS5)))));
            q = 1.0 + z * (arc_qS1 + z * (arc_qS2 + z * (arc_qS3 + z * arc_qS4)));
            r = p / q;
            return(pio2_hi - (a - (pio2_lo - a * r)));
        }
        /* x < -0.5 */
        else if (hx < 0) {
            z = (1.0 + a) * 0.5;
            p = z * (arc_pS0 + z * (arc_pS1 + z * (arc_pS2 + z * (arc_pS3 + z * (arc_pS4 + z * arc_pS5)))));
            q = 1.0 + z * (arc_qS1 + z * (arc_qS2 + z * (arc_qS3 + z * arc_qS4)));
            s = Math.sqrt(z);
            r = p / q;
            w = r * s - pio2_lo;
            return(Math.PI - 2.0 * (s + w));
        }
        /* x > 0.5 */
        else {
            z = (1.0 - a) * 0.5;
            s = Math.sqrt(z);
            df = s;
            df = Double.longBitsToDouble(Double.doubleToLongBits(df) & high_bits_mask);
            c = (z - df * df) / (s + df);
            p = z * (arc_pS0 + z * (arc_pS1 + z * (arc_pS2 + z * (arc_pS3 + z * (arc_pS4 + z * arc_pS5)))));
            q = 1.0 + z * (arc_qS1 + z * (arc_qS2 + z * (arc_qS3 + z * arc_qS4)));
            r = p / q;
            w = r * s + c;
            return(2.0 * (df + w));
        }
    }
    
    
    /* arctan */
    
    private static final double atanhi[]= {
        4.63647609000806093515e-01, /* atan(0.5)hi 0x3FDDAC67, 0x0561BB4F */
        7.85398163397448278999e-01, /* atan(1.0)hi 0x3FE921FB, 0x54442D18 */
        9.82793723247329054082e-01, /* atan(1.5)hi 0x3FEF730B, 0xD281F69B */
        1.57079632679489655800e+00  /* atan(inf)hi 0x3FF921FB, 0x54442D18 */
    };
    
    private static final double atanlo[]= {
        2.26987774529616870924e-17, /* atan(0.5)lo 0x3C7A2B7F, 0x222F65E2 */
        3.06161699786838301793e-17, /* atan(1.0)lo 0x3C81A626, 0x33145C07 */
        1.39033110312309984516e-17, /* atan(1.5)lo 0x3C700788, 0x7AF0CBBD */
        6.12323399573676603587e-17  /* atan(inf)lo 0x3C91A626, 0x33145C07 */
    };
    
    private static final double aT[]= {
         3.33333333333329318027e-01, /* 0x3FD55555, 0x5555550D */
        -1.99999999998764832476e-01, /* 0xBFC99999, 0x9998EBC4 */
         1.42857142725034663711e-01, /* 0x3FC24924, 0x920083FF */
        -1.11111104054623557880e-01, /* 0xBFBC71C6, 0xFE231671 */
         9.09088713343650656196e-02, /* 0x3FB745CD, 0xC54C206E */
        -7.69187620504482999495e-02, /* 0xBFB3B0F2, 0xAF749A6D */
         6.66107313738753120669e-02, /* 0x3FB10D66, 0xA0D03D51 */
        -5.83357013379057348645e-02, /* 0xBFADDE2D, 0x52DEFD9A */
         4.97687799461593236017e-02, /* 0x3FA97B4B, 0x24760DEB */
        -3.65315727442169155270e-02, /* 0xBFA2B444, 0x2C6A6C2F */
         1.62858201153657823623e-02, /* 0x3F90AD3A, 0xE322DA11 */
    };
    
    /**
     * Returns the arc tangent of an angle, in the range of -<i>pi</i>/2 through <i>pi</i>/2. 
     * Special cases:
     * <ul>
     *  <li>If the argument is NaN, then the result is NaN.
     *  <li>If the argument is zero, then the result is a zero with the same sign as the argument.
     * </ul>
     *
     * @param a the value whose arc tangent is to be returned.
     * @return the arc tangent of the argument.
     */
    public static double atan(double a) {
        double w,s1,s2,z;
        int id;
        long hx = Double.doubleToLongBits(a);
        long ix = hx & no_sign_mask;
        
        /* |a| >= 2^66 */
        if (ix >= 0x4410000000000000L) {
            /* NaN */
            if (ix > exp_mask) {
                return(a + a);
            }
            if (hx > 0) return(atanhi[3] + atanlo[3]);
            else return(-atanhi[3] - atanlo[3]);
        }
        
        /* |a| < 0.4375 */
        if (ix < 0x3fdc000000000000L) {
            /* |a| < 2^-29 */
            if (ix < 0x3e20000000000000L) {
                return(a);
            }
            id = -1;
        } else {
            a = Math.abs(a);
            /* |a| < 1.1875 */
            if (ix < 0x3ff3000000000000L) {
                /* 7/16 <= |a| < 11/16 */
                if (ix < 0x3fe6000000000000L) {
                    id = 0;
                    a = (2.0 * a - 1.0) / (2.0 + a);
                }
                /* 11/16 <= |a| < 19/16 */
                else {
                    id = 1;
                    a = (a - 1.0) / (a + 1.0);
                }
            } else {
                /* |x| < 2.4375 */
                if (ix < 0x4003800000000000L) {
                    id = 2;
                    a = (a - 1.5) / (1.0 + 1.5 * a);
                } else {
                    id = 3;
                    a = -1.0 / a;
                }
            }
        }
        
        /* end of argument reduction */
        z = a * a;
        w = z * z;
        
        /* break sum from i=0 to 10 into odd and even poly */
        s1 = z * (aT[0] + w * (aT[2] + w * (aT[4] + w * (aT[6] + w * (aT[8] + w * aT[10])))));
        s2 = w * (aT[1] + w * (aT[3] + w * (aT[5] + w * (aT[7] + w * aT[9]))));
        if (id < 0) return (a - a * (s1 + s2));
        else {
            z = atanhi[id] - ((a * (s1 + s2) - atanlo[id]) - a);
            if (hx < 0) return(-z);
            else return(z);
        }
    }
    
    
    /**
     * Converts rectangular coordinates (<i>x</i>, <i>y</i>) to polar (<i>r</i>, <i>theta</i>).
     * This method computes the phase <i>theta</i> by computing an arc tangent
     * of <i>y</i>/<i>x</i> in the range of -<i>pi</i> to <i>pi</i>. Special cases:
     * <ul>
     *  <li>If either argument is NaN, then the result is NaN.
     *  <li>If the first argument is positive zero and the second argument is positive, 
     *      or the first argument is positive and finite and the second argument is positive infinity,
     *      then the result is positive zero.
     *  <li>If the first argument is negative zero and the second argument is positive,
     *      or the first argument is negative and finite and the second argument is positive infinity,
     *      then the result is negative zero.
     *  <li>If the first argument is positive zero and the second argument is negative,
     *      or the first argument is positive and finite and the second argument is negative infinity,
     *      then the result is the double value closest to <i>pi</i>.
     *  <li>If the first argument is negative zero and the second argument is negative,
     *      or the first argument is negative and finite and the second argument is negative infinity,
     *      then the result is the double value closest to -<i>pi</i>.
     *  <li>If the first argument is positive and the second argument is positive zero or negative zero,
     *      or the first argument is positive infinity and the second argument is finite,
     *      then the result is the double value closest to <i>pi</i>/2.
     *  <li>If the first argument is negative and the second argument is positive zero or negative zero,
     *      or the first argument is negative infinity and the second argument is finite,
     *      then the result is the double value closest to -<i>pi</i>/2.
     *  <li>If both arguments are positive infinity, then the result is the double value closest to <i>pi</i>/4.
     *  <li>If the first argument is positive infinity and the second argument is negative infinity,
     *      then the result is the double value closest to 3*<i>pi</i>/4.
     *  <li>If the first argument is negative infinity and the second argument is positive infinity,
     *      then the result is the double value closest to -<i>pi</i>/4.
     *  <li>If both arguments are negative infinity, then the result is the double value closest to -3*<i>pi</i>/4.
     * </ul>
     *
     * @param y the ordinate coordinate
     * @param x the abscissa coordinate
     *
     * @return the <i>theta</i> component of the point (<i>r</i>, <i>theta</i>) in polar coordinates
     *         that corresponds to the point (<i>x</i>, <i>y</i>) in Cartesian coordinates.
     */
    public static double atan2(double y, double x) {
        double pi_o_4  = 7.8539816339744827900E-01; /* 0x3FE921FB, 0x54442D18 */
        double pi_o_2  = 1.5707963267948965580E+00; /* 0x3FF921FB, 0x54442D18 */
        double pi      = 3.1415926535897931160E+00; /* 0x400921FB, 0x54442D18 */
        double pi_lo   = 1.2246467991473531772E-16; /* 0x3CA1A626, 0x33145C07 */
        double z;
        int k,m;
        long hx = Double.doubleToLongBits(x);
        long hy = Double.doubleToLongBits(y);
        long ix = (hx & no_sign_mask);
        long iy = (hy & no_sign_mask);
        
        
        /* x or y is NaN */
        if ((Double.isNaN(x)) || (Double.isNaN(y))) {
            return(x + y);
        }
        
        /* x = 1.0 */
        if (hx == one) return(atan(y));
        
        /* 2 * sign(x) + sign(y) */
        m = (int)(((hy >> 63) & 1) | ((hx >> 62) & 2));
        
        /* when y = 0 */
        if (iy == 0) {
            switch(m) {
                case 0:
                case 1: return(y);             /* atan(+-0, +anything) = +- 0 */
                case 2: return(pi);            /* atan( +0, -anything) =  pi */
                case 3: return(-pi);           /* atan( -0, -anything) = -pi */
            }
        }
        
        /* when x = 0 */
        if (ix == 0) {
            if (hy < 0) return(-pi_o_2);
            else return(pi_o_2);
        }
        
        /* when x is INF */
        if (Double.isInfinite(x)) {
            if (Double.isInfinite(y)) {
                switch(m) {
                    case 0: return(pi_o_4);           /* atan(+inf, +inf) */
                    case 1: return(-pi_o_4);          /* atan(-inf, +inf) */
                    case 2: return(3.0 * pi_o_4);     /* atan(+inf, -inf) */
                    case 3: return(-3.0 * pi_o_4);    /* atan(-inf, -inf) */
                }
            } else {
                switch(m) {
                    case 0: return(0.0);
                    case 1: return(-0.0);
                    case 2: return(pi);
                    case 3: return(-pi);
                }
            }
        }
        
        /* when y is inf */
        if (Double.isInfinite(y)) {
            if (hy < 0) return(-pi_o_2);
            else return(pi_o_2);
        }
        
        /* compute y / x */
        k = (int)((iy - ix) >> 52);
        if (k > 60) z = pi_o_2 + 0.5 * pi_lo;       /* |y/x| > 2^60 */
        else if ((hx < 0) && (k < -60)) z = 0.0;    /* |y/x| < -2^60 */
        else z = atan(Math.abs(y / x));             /* safe to do y/x */
        
        switch(m) {
            case 0: return(z);                      /* atan(+,+) */
            case 1: return(-z);                     /* atan(-,+) */
            case 2: return(pi - (z - pi_lo));       /* atan(+,-) */
            default: /*case 3*/
                return((z - pi_lo) - pi);           /* atan(-,-) */
        }
    }

}
