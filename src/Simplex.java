import org.apache.commons.math3.fraction.Fraction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Simplex {

    private Fraction[][] program;
    private List<Fraction[][]> iterations;

    public Simplex() throws IOException {
        iterations = new ArrayList<Fraction[][]>();
        fetchInput();
        solve();
        printSolution();
    }

    public void fetchInput() throws IOException {
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Input number of variables:   ");
        int variables   = Integer.parseInt(bufferRead.readLine());
        System.out.print("Input number of constraints: ");
        int constraints = Integer.parseInt(bufferRead.readLine());

        program = new Fraction[constraints+1][variables+constraints+1];

        System.out.println("Input the variables of each constraint space delimited");
        System.out.println("The last must be the objective function");
        System.out.println("Ex 4 1 1 9 means 4x + y + z <= 9");
        for (int i = 0; i < constraints+1; i++) {
            System.out.print("Input constraint: ");
            String[] constraint = bufferRead.readLine().split(" ");
            // Insert the variables into the program matrix
            for (int j = 0; j < variables; j++)
                program[i][j] = new Fraction(Integer.parseInt(constraint[j]));
            // Insert the slack variables into the program matrix
            for (int j = 0; j < constraints; j++)
                program[i][variables+j] = new Fraction(j==i ? 1 : 0);
            // Insert last value, but not to the objective function
            if (i != constraints) program[i][variables+constraints] = new Fraction(Integer.parseInt(constraint[variables]));
            else program[i][variables+constraints] = new Fraction(0);
        }

        System.out.println("\nLinear Program to solve:");
        printProgram();
    }

    public void solve() {
        int pivotCol;
        while ((pivotCol = getpivotCol()) != -1) {
            int pivotRow = getpivotRow(pivotCol);
            if (pivotRow == -1) {
                System.out.println("THE PROBLEM IS UNBOUNDED");
                System.exit(1);
            }
            Fraction pivotNumber = program[pivotRow][pivotCol];

            // Divide every entry in the pivot row by the pivot number
            Fraction[] pRow = program[pivotRow];
            for (int i = 0; i < pRow.length; i++) {
                program[pivotRow][i] = pRow[i].divide(pivotNumber);
            }

            // Subtract a multiple of the pivot row form all other rows to the entry
            // in the pivot column becomes zero
            for (int i = 0; i < program.length; i++) {
                if (i == pivotRow) continue; // Skip the pivot row itself
                // Find the suitable multiple
                Fraction[] row = program[i];
                Fraction multiple = row[pivotCol].negate();
                // Add the multiple the pivot row to every index
                for (int j = 0; j < row.length; j++) {
                    row[j] = row[j].add(pRow[j].multiply(multiple));
                }
            }

            System.out.println("\nThe pivot row position was: (" + pivotCol + "," + pivotRow + ") = " + pivotNumber);
            printProgram();
        }
    }

    private void printSolution() {
        System.out.println("\nTHE SOLUTION IS z = " + program[program.length-1][program[0].length-1].negate());
    }

    private int getpivotCol() {
        Fraction[] lastrow = program[program.length-1];
        Fraction best = new Fraction(0);
        int pivotCol = -1;
        for (int i = 0; i < lastrow.length - 1; i++) { // -1 because we don't want to consider the last value
            if (lastrow[i].compareTo(best) > 0 ) {
                best     = lastrow[i];
                pivotCol = i;
            }
        }
        return pivotCol;
    }
    
    private int getpivotRow(int pivotCol) {
        System.out.println("Getting pivotRow for column " + pivotCol);
        Fraction smallestRatio = Fraction.MINUS_ONE;
        int pivotRow = -1;
        for (int i = 0; i < program.length-1; i++) {
            System.out.println("Trying row " + i);
            Fraction value = program[i][pivotCol];
            if (value.equals(Fraction.ZERO)) continue; // Never go full retard!
            Fraction ratio = program[i][program[i].length-1].divide(value);
            if (value.compareTo(Fraction.ZERO) > 0
                    && (ratio.compareTo(smallestRatio) < 0 || smallestRatio.equals(Fraction.MINUS_ONE))) {
                smallestRatio = ratio;
                pivotRow = i;
            }
        }
        return pivotRow;
    }

    private void printProgram() {
        for (int i = 0; i < program.length; i++) {
            for (int j = 0; j < program[i].length; j++) {
                System.out.print("\t" + program[i][j]);
            }
            System.out.println();
        }
    }

    public static void main(String[] args) throws IOException {
        Simplex s = new Simplex();
        s.solve();

    }

}
