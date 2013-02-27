import org.apache.commons.math3.fraction.Fraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Simplex {

    private Fraction[][] program;
    private List<Fraction[][]> iterations;
    private List<int[]> basisHistory;
    private int[] basis;

    public Simplex(String programString, String templateFile, String outputfile, String substitutionString) throws IOException {
        iterations   = new ArrayList<Fraction[][]>();
        basisHistory = new ArrayList<int[]>();

        program = stringToProgram(programString);

        System.out.println("\nLinear Program to solve:");
        printMatrix(program);

        solve();
        String latex = generateLatex();

        writeToFile(latex, templateFile, outputfile, substitutionString);
    }

    private void writeToFile(String str, String templateFile, String outputfile, String substitutionString) throws IOException {
        Path path = FileSystems.getDefault().getPath(".", templateFile);
        List<String> lines = Files.readAllLines(path, Charset.defaultCharset());

        Path newPath = FileSystems.getDefault().getPath(".", outputfile);
        BufferedWriter writer = Files.newBufferedWriter(newPath, Charset.defaultCharset(), StandardOpenOption.CREATE);
        for (String line : lines) {
            if (line.equals(substitutionString)) line = str;
            else line += "\n";
            writer.write(line, 0, line.length());
        }

        writer.close();
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
        basisHistory.add(Arrays.copyOf(basis, basis.length));

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
                program[i][variables+j] = j==i ? Fraction.ONE : Fraction.ZERO;
            // Insert last value, but not to the objective function
            if (i != constraints) program[i][variables+constraints] = new Fraction(Integer.parseInt(constraint[variables]));
            else program[i][variables+constraints] = Fraction.ZERO;
        }

        System.out.println("\nLinear Program to solve:");
        printMatrix(program);
    }

    private void solve() {
        addToIterations(program);

        int pivotCol;
        while ((pivotCol = getpivotCol()) != -1) {
            int pivotRow = getpivotRow(pivotCol);

            // Update the basis
            basis[pivotRow] = pivotCol;
            basisHistory.add(Arrays.copyOf(basis, basis.length));

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

            addToIterations(program);
            System.out.println("\nThe pivot row position was: (" + pivotCol + "," + pivotRow + ") = " + pivotNumber);
        }
    }

    private String generateLatex() {
        StringBuilder rows = new StringBuilder();

        int counter = 0;
        for (Fraction[][] matrix : iterations) {
            int[] basis = basisHistory.get(counter);

            rows.append(String.format("\\textbf{Iteration %d}\n", counter+1));
            rows.append("\\begin{alignat*}{"+(matrix[0].length+1)+"}\n");

            for (int rowIndex = 0; rowIndex < matrix.length-1; rowIndex++) {
                String row = "";
                Fraction[] currentRow = matrix[rowIndex];


                row += "\tx_{" + basis[rowIndex] + "} &={}& " + currentRow[currentRow.length-1] + " ";
                for (int index = 0; index < currentRow.length-1; index++) {
                    // Skip when the coefficient is zero
                    if (currentRow[index].equals(Fraction.ZERO) || index == basis[rowIndex]) {
                        row += "&{}&    ";
                        continue;
                    }

                    String sign = "+";
                    Fraction coefficient = currentRow[index];
                    if (coefficient.compareTo(Fraction.ZERO) < 0) {
                        // The coefficient is negative
                        sign = "-";
                        coefficient = coefficient.negate();
                    }

                    if (coefficient.equals(Fraction.ONE)) {
                        // Don't write the coefficient if it is one
                        row += String.format("&%s{}&   x_{%d} ", sign, index);
                    } else {
                        row += String.format("&%s{}&   %sx_{%d} ", sign, formatCoefficient(coefficient), index);
                    }
                }

                row += "\\\\\n";
                rows.append(row);
            }

            Fraction[] currentRow = matrix[matrix.length-1];
            rows.append("\tz &={}& ");
            for (int index = 0; index < currentRow.length-1; index++) {
                // Skip when the coefficient is zero
                if (currentRow[index].equals(Fraction.ZERO)) {
                    rows.append("&{}&    ");
                    continue;
                }

                String sign = "+";
                Fraction coefficient = currentRow[index];
                if (coefficient.compareTo(Fraction.ZERO) < 0) {
                    // The coefficient is negative
                    sign = "-";
                    coefficient = coefficient.negate();
                }
                rows.append(String.format("&%s{}&   %sx_{%d} ", sign, formatCoefficient(coefficient), index));
            }
            rows.append("\n");

            rows.append("\\end{alignat*}\n\n");
            counter++;
        }

        rows.append("\\textbf{The optimal solution}\n");
        rows.append("\\begin{equation*}\n");

        // Print x values
        Fraction[] values = new Fraction[program[0].length-1];
        for (int i = 0; i < program[0].length-1; i++) values[i] = Fraction.ZERO;

        for (int i = 0; i < basis.length; i++) {
            int var = basis[i];
            values[var] = program[i][program[0].length-1];
        }

        int vars = program[0].length - program.length;
        for (int i = 0; i < vars; i++) {
            if (i > 0) rows.append(", \\quad ");
            rows.append("\tx_"+(i+1)+" = " + formatCoefficient(values[i]));
        }

        Fraction solution = program[program.length - 1][program[0].length - 1].negate();
        rows.append("\\end{equation*}\n")
                .append("and it yields $z = ")
                .append(formatCoefficient(solution))
                .append("$.");


        return rows.toString();
    }

    private String formatCoefficient(Fraction coefficient) {
        int num = coefficient.getNumerator();
        int den = coefficient.getDenominator();

        if (num % den == 0) {
            // The coefficient is an integer
            return coefficient.toString();
        } else {
            return String.format("\\frac{%d}{%d}", num, den);
        }
    }

    private void printSolution() {
        System.out.println("\nTHE SOLUTION IS z = " + program[program.length - 1][program[0].length - 1].negate());

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

    private void printMatrix(Fraction[][] matrix) {
        for (Fraction[] row : matrix) {
            for (Fraction index : row) System.out.print("\t" + index);
            System.out.println();
        }
    }

    private void addToIterations(Fraction[][] matrix) {
        Fraction[][] newMatrix = new Fraction[matrix.length][matrix[0].length];
        iterations.add(newMatrix);

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                newMatrix[i][j] = matrix[i][j];
            }
        }
    }

    public static void main(String[] args) throws IOException {
        //String test = "3 3 2 3 1 5 4 1 2 11 3 4 2 8 5 4 3";
        String test = "2 3 3 6 40 -1 3 0 1 4 16 100 300";
        new Simplex(test, "template.tex", "handin.tex", "% Insert Simplex Solution");
    }

}
