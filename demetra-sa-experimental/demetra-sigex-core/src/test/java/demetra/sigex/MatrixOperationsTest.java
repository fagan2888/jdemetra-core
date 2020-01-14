/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demetra.sigex;

import jdplus.math.matrices.Matrix;
import jdplus.math.matrices.SymmetricMatrix;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
public class MatrixOperationsTest {
    
    public MatrixOperationsTest() {
    }

    @Test
    public void testGcd() {
        Matrix m=Matrix.make(10,5 );
        Random rnd=new Random();
        double[] storage = m.getStorage();
        for (int i=0; i<storage.length; ++i)
            storage[i]=rnd.nextDouble();
        Matrix s=SymmetricMatrix.XXt(m);
        Matrix[] gcd = MatrixOperations.gcd(s, 10);
        System.out.println(gcd[0]);
        System.out.println(gcd[1]);
    }
    
}
