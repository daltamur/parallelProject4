import javax.crypto.AEADBadTagException;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class multiThreadServer extends JPanel implements  constants{

    public static region [][] paintedA;
    public static region [][] paintedB;
    public static boolean startPainting;
    public static boolean paintA;
    public static boolean paintB;

    @Override
    protected void paintComponent(Graphics g) {
        if(startPainting){
            super.paintComponent(g);
            int curXPos=0;
            int curYPos=0;
            // width will store the width of the screen
            int widthFrame =  frame.getSize().width;

            double widthMesh = Math.ceil((double) widthFrame/(double) paintedA[0].length);

            // height will store the height of the screen
            int heightFrame = frame.getSize().height;
            double heightMesh = Math.ceil((double) heightFrame/(double) paintedA.length);
            for (int i = 0; i<paintedA.length; i++){
                for(int j = 0; j<paintedA[0].length; j++){
                    double curTemp = 0.0;
                    if(paintB) {
                      curTemp = paintedB[i][j].getCurrentTemp();
                    }else{
                      curTemp = paintedA[i][j].getCurrentTemp();
                    }
                    if (curTemp > 255){
                        curTemp = 255.0;
                    }
                    //use the function y = -x + 255 to get the inverses where x=curTemp
                    curTemp = -curTemp + 255;
                    double hue = curTemp/360.0;

                    g.setColor(Color.getHSBColor((float) hue, 1, 1));

                    g.fillRect(curXPos, curYPos, (int) widthMesh, (int) heightMesh);
                    if(j == paintedA[0].length - 1){
                        curXPos = 0;
                        curYPos += heightMesh;
                    }else{
                        curXPos += widthMesh;
                    }

                }
            }
        }
    }


    @Override
    public Dimension getPreferredSize() {
        // so that our GUI is big enough
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        // width will store the width of the screen
        int widthV = gd.getFullScreenWindow().getWidth();

        // height will store the height of the screen
        int heightV = gd.getFullScreenWindow().getHeight();
        return new Dimension(widthV, heightV);
    }


    private static void createAndShowGui() {
        multiThreadServer mainPanel = new multiThreadServer();
        mainPanel.setLayout(new BorderLayout());
        JLabel title = new JLabel("Title");
        title.setBackground(Color.black);
        title.setFont(new Font(title.getFont().toString(), Font.BOLD, 48));
        title.setForeground(Color.white);
        title.setText("Heat Propagation Visualization of a Heated Metal Alloy");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel name = new JLabel("Name");
        name.setBackground(Color.black);
        name.setFont(new Font(title.getFont().toString(), Font.BOLD, 15));
        name.setForeground(Color.white);
        name.setText("Authored by Dominic Altamura");
        mainPanel.add(title, BorderLayout.PAGE_START);
        mainPanel.add(name,BorderLayout.SOUTH);
        GraphicsEnvironment graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = graphics.getDefaultScreenDevice();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mainPanel);
        //show a new floor every fifth of a second
        Timer t = new Timer(10,
                e -> mainPanel.repaint());
        t.start();
        frame.setVisible(true);
        beginProgram = true;
       GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
       Rectangle bounds = environment.getMaximumWindowBounds();
       System.out.println("Screen Bounds = " + bounds);
       GraphicsConfiguration config = device.getDefaultConfiguration();
       System.out.println("Screen Size = " + config.getBounds());
       System.out.println("Frame Size = " + frame.getSize());
    }

    public static JFrame frame;
    public static boolean beginProgram = false;


    public static void main(String[] args){
        frame = new JFrame("Jacobi Relaxation");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        //frame.setSize(1600, 1080);
        frame.setUndecorated(true);
        frame.setVisible(true);
        SwingUtilities.invokeLater(multiThreadServer::createAndShowGui);
        while (!beginProgram){
            System.out.println("Waiting on gui initialization");
        }
        //int height = 0;
        //int width = 0;
//
        //try {
        //    height = Integer.parseInt(JOptionPane.showInputDialog(null,"Height of mesh (preferrable if you make the height a divisor of " + frame.getSize().height));
        //}catch (Exception e){
        //    System.out.println(e);
        //    System.exit(-1);
        //}
//
        //try {
        //    width = Integer.parseInt(JOptionPane.showInputDialog(null,"Width of mesh (preferrable if you make the height a divisor of " + frame.getSize().width));
        //}catch (Exception e){
        //    System.out.println(e);
        //    System.exit(-1);
        //}
        //frame.setVisible(false);
        int height = 135;
        int width = 240;
        ServerSocket server = null;
        Phaser phaser = new Phaser();
        phaser.register();
        Phaser leafPhaser = new Phaser();
        leafPhaser.register();
        ExecutorService service = Executors.newWorkStealingPool();
        ArrayList<ClientHandler> clients = new ArrayList<>();
        ArrayList<Leaf> edges = new ArrayList<>();
        region[][] a = new region[height][width];
        region[][] b = new region[height][width];
        for(int i = 0; i<a.length; i++){
            for(int j = 0; j<a[0].length; j++){
                //for metal1Percentage, this one should never be a majority value, as it will always bring the temperature down if it is a majority.
                double metal1Portion = ThreadLocalRandom.current().nextDouble(0,1);
                double metal2Portion = ThreadLocalRandom.current().nextDouble(0, 1-metal1Portion);
                double metal3Portion = 1 - metal1Portion - metal2Portion;

                double currentTemp = 45.0;//ThreadLocalRandom.current().nextDouble(-100,100);
                a[i][j] = new region(false, metal1Portion, metal2Portion, metal3Portion, currentTemp);
                b[i][j] = new region(false, metal1Portion, metal2Portion, metal3Portion, currentTemp);
            }
        }
        a[0][0].changeTemp(S);
        a[0][0].setHeatedCorner(true);
        a[a.length-1][a[0].length-1].changeTemp(T);
        a[a.length-1][a[0].length-1].setHeatedCorner(true);
        b[0][0].changeTemp(S);
        b[0][0].setHeatedCorner(true);
        b[b.length-1][b[0].length-1].changeTemp(T);
        b[b.length-1][b[0].length-1].setHeatedCorner(true);
        paintedA = a;
        paintedB = b;
        startPainting = true;

        try{
            Leaf curEdge;
            server = new ServerSocket(2697);
            server.setReuseAddress(true);
            int numberOfClients = 3;
            int chunkWidth = a[0].length/numberOfClients;
            int widthStart = 0;
            int widthEnd = widthStart + chunkWidth-1;
            Socket q1Client = server.accept();
            //make array chunks for each client;
            region[][] aQ1 = new region[height][widthEnd - widthStart+1];
            region[][] bQ1 = new region[height][widthEnd - widthStart+1];
            for(int i = 0; i<a.length; i++){
                int curWidth = 0;
                for(int j = widthStart; j<=widthEnd; j++){
                    aQ1[i][curWidth] = a[i][j];
                    bQ1[i][curWidth] = b[i][j];
                    curWidth++;
                }
            }
            //move the j values
            curEdge = new Leaf(a, b, 0, a.length-1, widthEnd, widthEnd);
            ClientHandler q1 = new ClientHandler(q1Client, phaser, aQ1, bQ1, a, b, widthStart, widthEnd);
            edges.add(curEdge);
            curEdge = new Leaf(a, b, 0, a.length-1, widthEnd+1, widthEnd+1);
            edges.add(curEdge);
            widthStart = widthEnd+1;
            widthEnd = widthEnd + chunkWidth-1;
            Socket q2Client = server.accept();
            curEdge = new Leaf(a, b, 0, a.length-1, widthEnd, widthEnd);
            edges.add(curEdge);
            curEdge = new Leaf(a, b, 0, a.length-1, widthEnd+1, widthEnd+1);
            edges.add(curEdge);
            region[][] aQ2 = new region[height][widthEnd - widthStart+1];
            region[][] bQ2 = new region[height][widthEnd - widthStart+1];
            for(int i = 0; i<a.length; i++){
                int curWidth = 0;
                for(int j = widthStart; j<=widthEnd; j++){
                    aQ2[i][curWidth] = a[i][j];
                    bQ2[i][curWidth] = b[i][j];
                    curWidth++;
                }
            }
            ClientHandler q2 = new ClientHandler(q2Client, phaser, aQ2, bQ2, a, b, widthStart, widthEnd);
            //move the j values
            widthStart = widthEnd+1;
            widthEnd = widthEnd + chunkWidth-1;
            if(widthEnd>a[0].length-1){
                widthEnd = widthEnd - (widthStart - a[0].length-1);
            }else if(widthEnd<a[0].length-1){
                widthEnd = widthEnd + ((a[0].length-1) - widthEnd);
            }
            Socket q3Client = server.accept();
            region[][] aQ3 = new region[height][widthEnd - widthStart+1];
            region[][] bQ3 = new region[height][widthEnd - widthStart+1];
            for(int i = 0; i<a.length; i++){
                int curWidth = 0;
                for(int j = widthStart; j<=widthEnd; j++){
                    aQ3[i][curWidth] = a[i][j];
                    bQ3[i][curWidth] = b[i][j];
                    curWidth++;
                }
            }
            ClientHandler q3 = new ClientHandler(q3Client, phaser, aQ3, bQ3, a, b ,widthStart, widthEnd);
            //store the separete executables
            clients.add(q1);
            clients.add(q2);
            clients.add(q3);
            //execute them one by one
            runJacobiAlgorithm(a, b, edges, clients, phaser, service);


        } catch (IOException e) {
            e.printStackTrace();
        }





    }

    public static void runJacobiAlgorithm(region[][] A, region[][] B, ArrayList<Leaf> edges, ArrayList<ClientHandler> clients, Phaser phaser, ExecutorService service) throws IOException {
        int step = 0;
        while(true){
            double maxDifference = 0;
            for(ClientHandler c: clients){
                service.execute(c);
            }

            //do not go forward until all the clients have finished their work so far.
            phaser.arriveAndAwaitAdvance();

            //get the current maximumDifference after the clients do their work
            for(ClientHandler c: clients){
                //System.out.println("Max dif is " + c.getClientMaxDif());
                if(c.getClientMaxDif() > maxDifference){
                    maxDifference = c.getClientMaxDif();
                }
            }

            //we'll update the A and B array such that They have the new values from each client;


            //fork the leaves and reinitialize them again for later processing.
            for(Leaf edge: edges){
                edge.fork();
            }
            for(Leaf edge: edges){
                edge.join();
            }
            for(Leaf edge: edges){
                edge.reinitialize();
            }

            paintedB = B;
            paintedA = A;
            if(step % 2 ==0){
                paintB = true;
                paintA = false;
            }else{
                paintA = true;
                paintB = false;
            }
            // just a print test to make sure edges got updated on the client side of things
            /*
            for(Leaf edge: edges){
                for(int i = 0; i< edge.vals.length; i++){
                    for(int j = 0; j< edge.vals[0].length; j++)
                        System.out.println(edge.vals[i][j]);
                }
            }

             */

            //send each  client a reference to their respective edge
            for(int i = 0; i< edges.size(); i++){
                if(i == 0){
                    clients.get(0).sendEdge(edges.get(i).getVals());
                }else if(i == edges.size()-1){
                    clients.get(clients.size()-1).sendEdge(edges.get(i).getVals());
                }else{
                    clients.get(1).sendEdge(edges.get(i).getVals());
                }
            }

            //also update the maximum difference
            for(Leaf edge: edges){
                if(edge.getMaxDif() > maxDifference){
                    maxDifference = edge.getMaxDif();
                }
            }

            //send a reference to the new edges to the clients
            //System.out.println(step);
            if(step%100==0){
                System.out.println(step);
            }
            if(step == 350){
                System.out.println("B array");
                for(int i = 0; i<A.length; i++){
                    for (int j = 0; j<A[0].length; j++){
                        System.out.print("|");
                        System.out.print(String.format("%.02f", B[i][j].getCurrentTemp()));
                        System.out.print("|");
                    }
                    System.out.println();
                }
                System.out.println("Max difference: "+ maxDifference);
                System.out.println("After "+step + " steps");
                //do some clean up
                for(ClientHandler c: clients){
                    c.objOutput.writeObject(0);
                }
                System.exit(0);
            }
            step++;
        }



    }


    abstract static class MeshSection extends RecursiveAction {
        //all mesh sections must hold a value of the maximum difference among the separate sections
        double maxDifference;
        boolean isDone;
    }

    //leaf class made to handle the edges of the mesh.
    static class Leaf extends MeshSection {
        private final region[][] A; //values from old matrix
        private final region[][] B; //values from new matrix

        //the matrix we swap from one to the other will change based on if the current step is even or odd
        private final int lowRow;
        private final int lowColumn;
        private final int highColumn;
        private final int highRow;
        private int steps = 0;
        private final double[][] vals;



        Leaf(region[][] a, region[][] b,
             int lowRow, int highRow,
             int lowColumn, int highColumn) {
            A = a;
            B = b;
            this.lowRow = lowRow;
            this.lowColumn = lowColumn;
            this.highColumn = highColumn;
            this.highRow = highRow;
            vals = new double[highRow - lowRow+1][highColumn - lowColumn+1];
            int curMeshRow = lowRow;
            for(int i = 0; i<vals.length; i++){
                int curMeshCol = lowColumn;
                for(int j = 0; j<vals[0].length; j++){
                    vals[i][j] = A[curMeshRow][curMeshCol].getCurrentTemp();
                    curMeshCol++;
                }
                curMeshRow++;
            }
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
            int curEdgeRow = 0;
            for(int i = lowRow; i<=highRow; ++i){
                int curEdgeCol = 0;
                for(int j = lowColumn; j<=highColumn; ++j) {
                    //keep the heated corners constant
                    if(!a[i][j].getIsHeatedCorner() && j!= a[i].length-1){
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
                        vals[curEdgeRow][curEdgeCol] = finalNewTemp;
                    }
                    // increment the indicies for the transmitted edge arrays
                curEdgeCol++;
                }
                curEdgeRow++;
            }
            this.maxDifference = currentMaxDif;
            this.isDone = true;
        }

        public double getMaxDif(){
            return this.maxDifference;
        }

        public double[][] getVals(){
            return vals;
        }

    }

    public static class ClientHandler implements Runnable{
        private final Socket clientSocket;
        private final Phaser phaser;

        private final OutputStream output;
        private final ObjectOutputStream objOutput;
        private final InputStream inputStream;
        private final ObjectInputStream objectInputStream;

        private double clientMaxDif = 0.0;
        private int step = 0;
        private region[][] A;
        private region[][] B;
        region[][] parentA;
        region[][] parentB;
        int rewriteStart;
        int rewriteEnd;

        public ClientHandler(Socket clientSocket, Phaser phaser, region[][] A, region[][] B, region[][] parentA, region[][] parentB, int rewriteStart, int rewriteEnd) throws IOException {
            this.clientSocket = clientSocket;
            output = clientSocket.getOutputStream();
            objOutput = new ObjectOutputStream(output);
            inputStream = clientSocket.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            this.A = A;
            this.B = B;
            this.parentA = parentA;
            this.parentB = parentB;
            this.phaser = phaser;
            this.rewriteStart = rewriteStart;
            this.rewriteEnd = rewriteEnd;
            objOutput.writeObject(A);
            objOutput.flush();
            objOutput.writeObject(B);
            objOutput.flush();
            phaser.register();
        }

        @Override
        public void run() {
            try {
                objOutput.writeObject((Integer) 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //make the array that takes in the new mesh values A or B depending on the step
            try {
                if(step%2 == 0){
                    B = (region[][]) objectInputStream.readObject();
                }else {
                    A = (region[][]) objectInputStream.readObject();
                }
                //get the maximum difference associated with the client
                clientMaxDif = (double) objectInputStream.readObject();
                //System.out.println("new max dif: "+clientMaxDif);
               for(int i = 0; i<parentA.length; i++){
                   int sectionIndex = 0;
                   for(int j = rewriteStart; j<=rewriteEnd; j++){
                       if(step%2 == 0){
                           parentB[i][j].changeTemp(B[i][sectionIndex].getCurrentTemp());
                       }else {
                           parentA[i][j].changeTemp(A[i][sectionIndex].getCurrentTemp());
                       }
                       sectionIndex++;
                   }
               }
                step++;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            //arrive when the client brings back an Array
            phaser.arrive();
        }

        public double getClientMaxDif(){
            return clientMaxDif;
        }

        public region[][] getAVal(){
            return A;
        }

        public region[][] getBVal(){
            return B;
        }


        public  void sendEdge(double[][] edge) throws IOException {
            objOutput.reset();
            objOutput.writeObject((double [][]) edge);
        }
    }

}

