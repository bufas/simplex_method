import org.apache.commons.math3.fraction.Fraction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Simplex {

    private Fraction[][] program;
    private List<Fraction[][]> iterations;
    private int[] basis;

    public Simplex(String programString, String templateFile, String substitutionString) throws IOException {
        program = stringToProgram(programString);

        System.out.println("\nLinear Program to solve:");
        printProgram();

        iterations = new ArrayList<Fraction[][]>();
        //fetchInput();
        solve();
        printSolution();
        outputLatex();
    }

    private Fraction[][] stringToProgram(String program) {
        String[] input = program.split(" ");

        // Fetch number of variables and number of constraints
        int vs = Integer.parseInt(input[0]);
        int cs = Integer.parseInt(input[1]);
        Fraction[][] result = new Fraction[cs+1][vs+cs+1];

        // Define the basis
        basis = new int[cs];
        for (int i = 0; i < cs; i++) {
            basis[i] = vs+i;
        }

        // Build the program matrix
        for (int i = 0; i < cs+1; i++) {
            int start = 2 + (i * (vs + 1));

            for (int j = 0; j < vs; j++)
                result[i][j] = new Fraction(Integer.parseInt(input[start+j]));
            // Insert the slack variables into the program matrix
            for (int j = 0; j < cs; j++)
                result[i][vs+j] = new Fraction(j==i ? 1 : 0);
            // Insert last value, but not to the objective function
            if (i != cs) result[i][vs+cs] = new Fraction(Integer.parseInt(input[start + vs]));
            else result[i][vs+cs] = new Fraction(0);
        }

        return result;
    }

    private void fetchInput() throws IOException {
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

    private void solve() {
        int pivotCol;
        while ((pivotCol = getpivotCol()) != -1) {
            int pivotRow = getpivotRow(pivotCol);

            // Update the basis
            basis[pivotRow] = pivotCol;

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

            iterations.add(program);
            System.out.println("\nThe pivot row position was: (" + pivotCol + "," + pivotRow + ") = " + pivotNumber);
            printProgram();
        }
    }

    private void outputLatex() {

    }

    private void printSolution() {
        System.out.println("\nTHE SOLUTION IS z = " + program[program.length-1][program[0].length-1].negate());

        // Initialize array
        Fraction[] values = new Fraction[program[0].length-1];
        for (int i = 0; i < program[0].length-1; i++) values[i] = Fraction.ZERO;

        for (int i = 0; i < basis.length; i++) {
            int var = basis[i];
            values[var] = program[i][program[0].length-1];
        }

        for (int i = 0; i < program[0].length-1; i++) System.out.println("\tx_"+(i+1)+" = " + values[i]);
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
        Fraction smallestRatio = Fraction.MINUS_ONE;
        int pivotRow = -1;
        for (int i = 0; i < program.length-1; i++) {
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
        String test = "3 3 2 3 1 5 4 1 2 11 3 4 2 8 5 4 3";
        Simplex s = new Simplex(test, "template.tex", "% Insert Simplex Solution");
    }

}
