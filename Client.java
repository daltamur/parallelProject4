import java.io.*;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.RecursiveAction;


public class Client implements constants{

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("Connecting to Host...");
        Scanner scanner = new Scanner(System.in);
        System.out.println("Write Host's IP Address");
        String host = scanner.next();
        Socket sock = new Socket(host, 2697);
        System.out.println("Connected to Host");
        // get the input stream from the connected socket
        InputStream inputStream = sock.getInputStream();
        // create a DataInputStream so we can read data from it.
        // get the output stream from the socket.
        OutputStream outputStream = sock.getOutputStream();
        // create an object output stream from the output stream so we can send an object through it
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        region[][] a = (region[][]) objectInputStream.readObject();
        region[][] b = (region[][]) objectInputStream.readObject();

        int startingCol = 0;
        if(!a[0][0].getIsHeatedCorner()){
            startingCol = 1;
        }
        int endingCol = a[0].length-1;
        if(!a[a.length-1][a[0].length-1].getIsHeatedCorner()){
            endingCol--;
        }
        int granularity = java.lang.Thread.activeCount();
        engine engine = new engine(a, b, 0, a.length-1, startingCol, endingCol, granularity, objectInputStream, objectOutputStream);
        engine.invoke();
    }




    abstract static class MeshSection extends RecursiveAction {
        //all mesh sections must hold a value of the maximum difference among the separate sections
        double maxDifference;
        boolean isDone;

        public abstract void reset();
    }

    static class fourQuad extends MeshSection{
        //reference to the four quadrants of the current mesh section
        MeshSection q1;
        MeshSection q2;
        MeshSection q3;
        MeshSection q4;

        //child of MeshSection, so needs to use its super constructor
        fourQuad(){}

        //when we complete all the forks for each quadrant, find the maximum difference



        public void compute() {
            //fork each quadrant
            q4.fork();
            q3.fork();
            q2.fork();
            q1.fork();
            q4.join();
            q3.join();
            q2.join();
            q1.join();
            q1.reinitialize();
            q2.reinitialize();
            q3.reinitialize();
            q4.reinitialize();
            this.maxDifference = q1.maxDifference;
            this.maxDifference = Math.max(this.maxDifference, q2.maxDifference);
            this.maxDifference = Math.max(this.maxDifference, q3.maxDifference);
            this.maxDifference = Math.max(this.maxDifference, q4.maxDifference);
        }

        @Override
        public void reset() {
            q1.reset();
            q2.reset();
            q3.reset();
            q4.reset();
        }
    }


    static class Leaf extends MeshSection{
        private final region[][] A; //values from old matrix
        private final region[][] B; //values from new matrix

        //the matrix we swap from one to the other will change based on if the current step is even or odd
        private final int lowRow;
        private final int lowColumn;
        private final int highColumn;
        private final int highRow;
        private int steps = 0;



        Leaf(region[][] a, region[][] b,
             int lowRow, int highRow,
             int lowColumn, int highColumn) {
            A = a;
            B = b;
            this.lowRow = lowRow;
            this.lowColumn = lowColumn;
            this.highColumn = highColumn;
            this.highRow = highRow;
        }


        public void compute() {
            //AtoB checks if the current step is an even or odd number. If it is even, it'll be 0. Otherwise it'll be 1 or smth I think
            //note that the steps always start at 0, so it will always be from A to B at first
            boolean AtoB = (steps++ % 2) == 0;
            // if the current step is even, the array we are taking values from will be A, otherwise it will be B.
            region[][] a = (AtoB)? A : B;
            // if the current step is even, the array we are putting values into will be B, otherwise it will be A.
            region[][] b = (AtoB)? B : A;

            double currentMaxDif = 0;

            //this.maxDifference = currentMaxDif;

            for(int i = lowRow; i<=highRow; ++i){
                for(int j = lowColumn; j<=highColumn; ++j) {
                    //keep the heated corners constant
                    if(!a[i][j].getIsHeatedCorner()){
                        double numberOfNeighbors = 0.0;
                        //get what neighbors exist
                        boolean topLeft = i-1>=0 && j-1>=0;
                        numberOfNeighbors = (topLeft)? numberOfNeighbors+1.0 : numberOfNeighbors;
                        boolean topRight = i-1>=0 && j+1<=a[0].length-1;
                        numberOfNeighbors = (topRight)? numberOfNeighbors+1.0 : numberOfNeighbors;
                        boolean top = i-1 >=0;
                        numberOfNeighbors = (top)? numberOfNeighbors+1.0 : numberOfNeighbors;
                        boolean bottom = i+1 <=a.length-1;
                        numberOfNeighbors = (bottom)? numberOfNeighbors+1.0 : numberOfNeighbors;
                        boolean left = j-1 >= 0;
                        numberOfNeighbors = (left)? numberOfNeighbors+1.0 : numberOfNeighbors;
                        boolean right = j+1 <= a[0].length-1;
                        numberOfNeighbors = (right)? numberOfNeighbors+1.0 : numberOfNeighbors;
                        boolean bottomLeft = i+1 <=a.length-1 && j-1>=0;
                        numberOfNeighbors = (bottomLeft)? numberOfNeighbors+1.0 : numberOfNeighbors;
                        boolean bottomRight = i+1 <= a.length-1 && j+1 <= a[0].length-1;
                        numberOfNeighbors = (bottomRight)? numberOfNeighbors+1.0 : numberOfNeighbors;
                        double finalNewTemp;
                        double constantSummation = 0.0;
                        //the summation multiply neighbor sums by their thermal constant
                        for(int currentConstantIteration = 0; currentConstantIteration<3; currentConstantIteration++){
                            double neighborSummation = 0.0;
                            double currentConstant;
                            switch (currentConstantIteration){
                                case 0: currentConstant = c1;
                                    break;
                                case 1: currentConstant = c2;
                                    break;
                                case 2: currentConstant = c3;
                                    break;
                                default:
                                    throw new IllegalStateException("Unexpected value: " + currentConstantIteration);
                            }
                            if(top){
                                switch (currentConstantIteration){
                                    case 0: neighborSummation = neighborSummation + a[i-1][j].getCurrentTemp()*a[i-1][j].getMetal1Percentage();
                                        break;
                                    case 1: neighborSummation = neighborSummation + a[i-1][j].getCurrentTemp()*a[i-1][j].getMetal2Percentage();
                                        break;
                                    case 2: neighborSummation = neighborSummation + a[i-1][j].getCurrentTemp()*a[i-1][j].getMetal3Percentage();
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + currentConstantIteration);
                                }
                            }
                            if(bottom){
                                switch (currentConstantIteration){
                                    case 0: neighborSummation = neighborSummation + a[i+1][j].getCurrentTemp()*a[i+1][j].getMetal1Percentage();
                                        break;
                                    case 1: neighborSummation = neighborSummation + a[i+1][j].getCurrentTemp()*a[i+1][j].getMetal2Percentage();
                                        break;
                                    case 2: neighborSummation = neighborSummation + a[i+1][j].getCurrentTemp()*a[i+1][j].getMetal3Percentage();
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + currentConstantIteration);
                                }
                            }
                            if(left){

                                switch (currentConstantIteration){
                                    case 0: neighborSummation = neighborSummation + a[i][j-1].getCurrentTemp()*a[i][j-1].getMetal1Percentage();
                                        break;
                                    case 1: neighborSummation = neighborSummation + a[i][j-1].getCurrentTemp()*a[i][j-1].getMetal2Percentage();
                                        break;
                                    case 2: neighborSummation = neighborSummation + a[i][j-1].getCurrentTemp()*a[i][j-1].getMetal3Percentage();
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + currentConstantIteration);
                                }//neighborSummation += a[i][j-1].getCurrentTemp() * a[i][j-1].getMetal1Percentage() * a[i][j-1].getMetal2Percentage() * a[i][j-1].getMetal3Percentage();
                            }
                            if(right){
                                switch (currentConstantIteration){
                                    case 0: neighborSummation = neighborSummation + a[i][j+1].getCurrentTemp()*a[i][j+1].getMetal1Percentage();
                                        break;
                                    case 1: neighborSummation = neighborSummation + a[i][j+1].getCurrentTemp()*a[i][j+1].getMetal2Percentage();
                                        break;
                                    case 2: neighborSummation = neighborSummation + a[i][j+1].getCurrentTemp()*a[i][j+1].getMetal3Percentage();
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + currentConstantIteration);
                                }
                                //neighborSummation += a[i][j+1].getCurrentTemp() * a[i][j+1].getMetal1Percentage() * a[i][j+1].getMetal2Percentage() * a[i][j+1].getMetal3Percentage();
                            }
                            if(topLeft){
                                switch (currentConstantIteration){
                                    case 0: neighborSummation = neighborSummation + a[i-1][j-1].getCurrentTemp()*a[i-1][j-1].getMetal1Percentage();
                                        break;
                                    case 1: neighborSummation = neighborSummation + a[i-1][j-1].getCurrentTemp()*a[i-1][j-1].getMetal2Percentage();
                                        break;
                                    case 2: neighborSummation = neighborSummation + a[i-1][j-1].getCurrentTemp()*a[i-1][j-1].getMetal3Percentage();
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + currentConstantIteration);
                                }
                                //neighborSummation += a[i-1][j-1].getCurrentTemp() * a[i-1][j-1].getMetal1Percentage() * a[i-1][j-1].getMetal2Percentage() * a[i-1][j-1].getMetal3Percentage();
                            }
                            if(topRight){
                                switch (currentConstantIteration){
                                    case 0: neighborSummation = neighborSummation + a[i-1][j+1].getCurrentTemp()*a[i-1][j+1].getMetal1Percentage();
                                        break;
                                    case 1: neighborSummation = neighborSummation + a[i-1][j+1].getCurrentTemp()*a[i-1][j+1].getMetal2Percentage();
                                        break;
                                    case 2: neighborSummation = neighborSummation + a[i-1][j+1].getCurrentTemp()*a[i-1][j+1].getMetal3Percentage();
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + currentConstantIteration);
                                }
                                //neighborSummation += a[i-1][j+1].getCurrentTemp() * a[i-1][j+1].getMetal1Percentage() * a[i-1][j+1].getMetal2Percentage() * a[i-1][j+1].getMetal3Percentage();
                            }
                            if(bottomLeft){
                                switch (currentConstantIteration){
                                    case 0: neighborSummation = neighborSummation + a[i+1][j-1].getCurrentTemp()*a[i+1][j-1].getMetal1Percentage();
                                        break;
                                    case 1: neighborSummation = neighborSummation + a[i+1][j-1].getCurrentTemp()*a[i+1][j-1].getMetal2Percentage();
                                        break;
                                    case 2: neighborSummation = neighborSummation + a[i+1][j-1].getCurrentTemp()*a[i+1][j-1].getMetal3Percentage();
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + currentConstantIteration);
                                }
                                //neighborSummation += a[i+1][j-1].getCurrentTemp() * a[i+1][j-1].getMetal1Percentage() * a[i+1][j-1].getMetal2Percentage() * a[i+1][j-1].getMetal3Percentage();
                            }
                            if(bottomRight){
                                switch (currentConstantIteration){
                                    case 0: neighborSummation = neighborSummation + a[i+1][j+1].getCurrentTemp()*a[i+1][j+1].getMetal1Percentage();
                                        break;
                                    case 1: neighborSummation = neighborSummation + a[i+1][j+1].getCurrentTemp()*a[i+1][j+1].getMetal2Percentage();
                                        break;
                                    case 2: neighborSummation = neighborSummation + a[i+1][j+1].getCurrentTemp()*a[i+1][j+1].getMetal3Percentage();
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + currentConstantIteration);
                                }
                            }

                            neighborSummation = neighborSummation*currentConstant;
                            constantSummation += neighborSummation;
                        }
                        constantSummation = constantSummation/numberOfNeighbors;
                        finalNewTemp = constantSummation;
                        double thisMaxDif = Math.abs(b[i][j].getCurrentTemp()-finalNewTemp);
                        if(thisMaxDif > currentMaxDif){
                            currentMaxDif = thisMaxDif;
                        }
                        b[i][j].changeTemp(finalNewTemp);
                    }

                }
            }
            this.maxDifference = currentMaxDif;
            this.isDone = true;
        }

        @Override
        public void reset() {
            steps = 0;
        }
    }

    static class engine extends RecursiveAction{
        //the root mesh
        private MeshSection root;
        //the two mesh regions
        private region[][] A;
        private region[][] B;
        private int firstRow;
        private int firstColumn;
        private int lastColumn;
        private int steps = 0;
        private int granularity;
        private ArrayList<region[][]> regions = new ArrayList<>();
        int numberOfSegments = 0;
        int numOfZeroes = 0;
        private final ObjectInputStream getSignal;
        private final ObjectOutputStream sendSection;

        public engine(region[][] A, region[][] B, int firstRow, int lastRow, int firstColumn, int lastColumn, int granularity, ObjectInputStream getSignal, ObjectOutputStream sendSection) throws InterruptedException {
            this.A = A;
            this.B = B;
            regions.add(A);
            regions.add(B);
            this.firstRow = firstRow;
            this.firstColumn = firstColumn;
            this.lastColumn = lastColumn;
            this.granularity = granularity;
            this.getSignal = getSignal;
            this.sendSection = sendSection;
            root = build(this.A, this.B, this.firstRow, lastRow, this.firstColumn, this.lastColumn, this.granularity);
            System.out.println("Built using "+ numberOfSegments + " segments");
            System.out.println(numOfZeroes);
        }

        @Override
        protected void compute() {
            while(true) {
                //make sure we're actually running the computer method before going further
                int continueSignal = 0;
                try {
                    continueSignal = (int) getSignal.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                if(continueSignal == 1) {
                    root.invoke();
                    try {
                        sendSection.reset();
                       if(this.steps%2 == 0){
                           sendSection.writeObject(B);
                       }else{
                           sendSection.writeObject(A);
                       }
                        sendSection.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }

                    //now send a reference to the max difference of this section
                    try {
                        sendSection.reset();
                        sendSection.writeObject(root.maxDifference);
                        sendSection.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }

                    //now update the edges
                    if (A[0][0].getIsHeatedCorner()) {
                        try {
                            double[][] rightEdge = (double[][]) getSignal.readObject();
                            if (this.steps % 2 == 0) {
                                mergeRightEdge(B, rightEdge);
                            } else {
                                mergeRightEdge(A, rightEdge);
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        //for if the section is the right most seciton
                    } else if (A[A.length-1][A[0].length-1].getIsHeatedCorner()) {
                        try {
                            double[][] leftEdge = (double[][]) getSignal.readObject();
                            if (this.steps % 2 == 0) {
                                mergeLeftEdge(B, leftEdge);
                            } else {
                                mergeLeftEdge(A, leftEdge);
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //for all the middle sections
                        try {
                            double[][] leftEdge = (double[][]) getSignal.readObject();
                            double[][] rightEdge = (double[][]) getSignal.readObject();
                            if (steps % 2 == 0) {
                                mergeLeftEdge(B, leftEdge);
                                mergeRightEdge(B, rightEdge);
                            } else {
                                mergeLeftEdge(A, leftEdge);
                                mergeRightEdge(A, rightEdge);
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                /*
                System.out.println("A array");
                for(int i = 0; i<A.length; i++){
                    for (int j = 0; j<A[0].length; j++){
                        System.out.print("|");
                        System.out.print(String.format("%.02f", A[i][j].getCurrentTemp()));
                        System.out.print("|");
                    }
                    System.out.println();
                }

                System.out.println("B array");
                for(int i = 0; i<A.length; i++){
                    for (int j = 0; j<A[0].length; j++){
                        System.out.print("|");
                        System.out.print(String.format("%.02f", B[i][j].getCurrentTemp()));
                        System.out.print("|");
                    }
                    System.out.println();
                }

                 */
                    //System.out.println("reinitializing mesh...");
                    //iterate the step counter for when switching between meshes
                    this.steps++;
                    root.reinitialize();
                }else{
                    System.out.println("Mesh has converged");
                    System.out.println("max dif is " + root.maxDifference);
                    System.out.println("B array");
                    for(int i = 0; i<A.length; i++){
                        for (int j = 0; j<A[0].length; j++){
                            System.out.print("|");
                            System.out.print(String.format("%.02f", B[i][j].getCurrentTemp()));
                            System.out.print("|");
                        }
                        System.out.println();
                    }
                    System.exit(0);
                }
            }
        }

        public MeshSection build(region[][] a, region[][] b, int lr, int hr, int lc, int hc, int granularity){
            //get number of rows
            int rows = (hr - lr + 1);
            //get number of columns
            int cols = (hc - lc + 1);

            //get the midpoint of the columns and rows using the bitshift operation that divides by 2
            int midpointRows = (lr + hr) >>> 1;
            int midpointColumns = (lc + hc) >>> 1;

            //if the current section is less than or equal to the granularity, that is a leaf where we do actual calculations
            if (rows * cols <= granularity){
                ++numberOfSegments;
                return new Leaf(a, b, lr, hr, lc, hc);
            }else{
                fourQuad quad = new fourQuad();
                quad.q1 = build(a, b, lr, midpointRows, lc, midpointColumns, granularity);
                quad.q2 = build(a, b, lr, midpointRows, midpointColumns + 1, hc, granularity);
                quad.q3 = build(a, b, midpointRows + 1, hr, lc, midpointColumns, granularity);
                quad.q4 = build(a, b, midpointRows + 1, hr, midpointColumns + 1, hc, granularity);
                return quad;
            }
        }

        public void mergeLeftEdge(region[][] Mesh, double[][] edge){
                for(int i=0; i<edge.length; i++){
                    for (int j = 0; j< edge[0].length; j++) {
                        Mesh[i][0].changeTemp(edge[i][j]);
                    }
                }
        }

        public void mergeRightEdge(region[][] Mesh, double[][] edge){
            for(int i=0; i<edge.length; i++){
                for (int j = 0; j< edge[0].length; j++) {
                    Mesh[i][Mesh[0].length-1].changeTemp(edge[i][j]);
                }
            }
        }

        public void reset(){
            root.reset();
        }
    }


}
