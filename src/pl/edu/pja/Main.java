package pl.edu.pja;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {

        // Ustal siezke do pliku z danymi, Locale.US dla '.' w double
        String absolutePath = new File("").getAbsolutePath();
        File inputData = new File(absolutePath + "\\network_setup");
        Scanner scanner = new Scanner(inputData).useLocale(Locale.US);

        // wczytujemy ilosc warstw sieci
        int iloscWarstwSieci = scanner.nextInt();

        // wczytujemy ilosc wejsc plus ilosc neuronow dla kazdej warstwy
        int [] iloscNeuronow = new int [iloscWarstwSieci + 1]; // +1 dla wejscia
        iloscNeuronow[0] = scanner.nextInt(); //ilosc wejsc
        int maxNeuronow = 0;
        for (int i = 1; i < iloscNeuronow.length; ++i) {
            iloscNeuronow[i] = scanner.nextInt();
            if(iloscNeuronow[i] > maxNeuronow)
                maxNeuronow = iloscNeuronow[i];
        }
        int maxWejsc = Math.max(maxNeuronow, iloscNeuronow[0]);

        // wczytujemy wszyskie wagi razem z biasami dla kazdego neuronu w kazdej warstwie
        double [][][] siecNeuronowa = new double [iloscWarstwSieci][maxNeuronow][maxWejsc + 1]; // +1 dla biasu
        Random random = new Random();
        for (int i = 0; i < iloscWarstwSieci; ++i) {
            for (int j = 0; j < iloscNeuronow[i + 1]; ++j) {
                for (int k = 0; k < iloscNeuronow[i]; ++k) {
                    siecNeuronowa[i][j][k] = random.nextDouble();
                }
                siecNeuronowa[i][j][siecNeuronowa[i][j].length - 1] = 0; // bias
            }
        }

        // wczytujemy lambde
        double lambda = scanner.nextDouble();

        // wczytujemy wspolczynnik uczenia
        double wspolczynnikUczenia = scanner.nextDouble();

        // zamykamy scanner dla wczytywania ustawien sieci
        scanner.close();

        // tworzymy scanner dla pliku z danymi znormalizowanymi
        File plikDanych = new File(absolutePath + "\\normalized_by_sensor_readings_24.data");
        Scanner scannerDoDanych = new Scanner(plikDanych).useLocale(Locale.US);
        int iloscSensorow = 24;
        int iloscLinii = 5434;
        // tworzymy tablice do przechowywania danych oraz tablice do przechowywania oczekiwanego ruchu
        double [][] tablicaDanychZnormalizowanych = new double [iloscLinii][iloscSensorow + 1];
        String [] oczekiwanyRuch = new String [iloscLinii];
        // wczytujemy dane do tablicy danych, 25 wpis z pliku laduje do tablicy oczekiwanego ruchu
        for (int i = 0; i < iloscLinii; ++i) {
            for (int j = 0; j < iloscSensorow; ++j) {
                tablicaDanychZnormalizowanych[i][j] = scannerDoDanych.nextDouble();
            }
            tablicaDanychZnormalizowanych[i][tablicaDanychZnormalizowanych[i].length - 1] = 1;
            oczekiwanyRuch[i] = scannerDoDanych.next();
        }

        double [] oczekiwaneSygnaly = new double [siecNeuronowa[iloscWarstwSieci-1].length];
        // Slight-Left-Turn, Move-Forward, Slight-Right-Turn, Sharp-Right-Turn
        double[]    slighLeft   = {1,0,0,0},
                    forward     = {0,1,0,0},
                    slightRight = {0,0,1,0},
                    sharpRight  = {0,0,0,1};

        // tworzymy tablice wynikowa dla przejscia przez siec
        double [][] przejsciePrzod = new double [iloscWarstwSieci + 1][maxWejsc + 1];

        // tworzymy tablice bledu neuronow
        double [][] bledyNeuronow = new double [iloscWarstwSieci][maxNeuronow];

        double funkcjaBledu = 0;

        int learningLines = (int)(iloscLinii * 0.7);

        int ileRazyUczyc = 40;
//==============================================================================================================
// =============================================================================================================

        System.out.println();
        System.out.println("Faza nauczania 70%");
        for(int big_index = 0; big_index < ileRazyUczyc; big_index++) {
            for (int index = 0; index < learningLines + 1; index++) {

                for (int i = 0; i < tablicaDanychZnormalizowanych[index].length; ++i) {
                    przejsciePrzod[0][i] = tablicaDanychZnormalizowanych[index][i];
                }
                for (int i = 0; i < siecNeuronowa.length; ++i) {
                    for (int j = 0; j < iloscNeuronow[i + 1]; ++j) {
                        for (int k = 0; k < iloscNeuronow[i] + 1; k++) {
                            przejsciePrzod[i + 1][j] += siecNeuronowa[i][j][k] * przejsciePrzod[i][k];
                        }
                        przejsciePrzod[i + 1][j] = 1 / (1 + Math.exp(lambda * przejsciePrzod[i + 1][j] * (-1)));

                    }
                    przejsciePrzod[i + 1][przejsciePrzod[i + 1].length - 1] = 1;
                }


                switch (oczekiwanyRuch[index]) {
                    case "Slight-Left-Turn":
                        oczekiwaneSygnaly = slighLeft;
                        break;
                    case "Move-Forward":
                        oczekiwaneSygnaly = forward;
                        break;
                    case "Slight-Right-Turn":
                        oczekiwaneSygnaly = slightRight;
                        break;
                    case "Sharp-Right-Turn":
                        oczekiwaneSygnaly = sharpRight;
                        break;
                }



                // przeliczamy bledy dla neuronow i zapisujemy je w tablicy, najpierw ostatnia warstwa
                for (int i = 0; i < iloscNeuronow[iloscNeuronow.length - 1]; ++i) {
                    bledyNeuronow[iloscWarstwSieci - 1][i] = (oczekiwaneSygnaly[i] - przejsciePrzod[iloscWarstwSieci][i]) *
                            wspolczynnikUczenia * przejsciePrzod[iloscWarstwSieci][i] * (1 - przejsciePrzod[iloscWarstwSieci][i]);
                }

                for (int i = iloscWarstwSieci - 1; i > 0; --i) {
                    for (int j = 0; j < siecNeuronowa[i].length; ++j) {
                        for (int k = 0; k < iloscNeuronow[i + 1]; ++k) {
                            bledyNeuronow[i - 1][j] += bledyNeuronow[i][k] * siecNeuronowa[i][k][j];
                        }
                        bledyNeuronow[i - 1][j] *= wspolczynnikUczenia * przejsciePrzod[i][j] * (1 - przejsciePrzod[i][j]);
                    }
                }


                for (int i = 0; i < iloscWarstwSieci; ++i) {
                    for (int j = 0; j < siecNeuronowa[i].length; ++j) {
                        for (int k = 0; k < siecNeuronowa[i][j].length - 1; ++k) {
                            siecNeuronowa[i][j][k] += (wspolczynnikUczenia * bledyNeuronow[i][j] * przejsciePrzod[i][j]);
                        }
                        siecNeuronowa[i][j][siecNeuronowa[i][j].length - 1] += wspolczynnikUczenia * bledyNeuronow[i][j];
                    }
                }


                for (int i = 0; i < iloscNeuronow[iloscNeuronow.length - 1]; ++i) {
                    funkcjaBledu += (Math.pow((oczekiwaneSygnaly[i] - przejsciePrzod[przejsciePrzod.length - 1][i]),2))/2;
                }


            }
            System.out.println(funkcjaBledu);
            funkcjaBledu = 0;
        }

        System.out.println();
        System.out.println("Faza testowania. 30%");
        funkcjaBledu = 0;
        int licznikPrawidlowychOdpowiedzi = 0;
        for (int big_index = 0; big_index < 1; big_index++) {
            funkcjaBledu = 0;
            for (int index = learningLines + 1; index < iloscLinii; index++) {

                for (int i = 0; i < tablicaDanychZnormalizowanych[index].length; ++i) {
                    przejsciePrzod[0][i] = tablicaDanychZnormalizowanych[index][i];
                }
                for (int i = 0; i < siecNeuronowa.length; ++i) {
                    for (int j = 0; j < iloscNeuronow[i + 1]; ++j) {
                        for (int k = 0; k < iloscNeuronow[i] + 1; k++) {
                            przejsciePrzod[i + 1][j] += siecNeuronowa[i][j][k] * przejsciePrzod[i][k];
                        }
                        przejsciePrzod[i + 1][j] = 1 / (1 + Math.exp(lambda * przejsciePrzod[i + 1][j] * (-1)));

                    }
                    przejsciePrzod[i + 1][przejsciePrzod[i + 1].length - 1] = 1;
                }

                switch (oczekiwanyRuch[index]) {
                    case "Slight-Left-Turn":
                        oczekiwaneSygnaly = slighLeft;
                        if(     przejsciePrzod[przejsciePrzod.length-1][0] >
                                Math.max(Math.max(przejsciePrzod[przejsciePrzod.length-1][1],
                                        przejsciePrzod[przejsciePrzod.length-1][2]),
                                        przejsciePrzod[przejsciePrzod.length-1][3])){
                                ++licznikPrawidlowychOdpowiedzi;
                        }
                        break;
                    case "Move-Forward":
                        oczekiwaneSygnaly = forward;
                        if(     przejsciePrzod[przejsciePrzod.length-1][1] >
                                Math.max(Math.max(przejsciePrzod[przejsciePrzod.length-1][0],
                                        przejsciePrzod[przejsciePrzod.length-1][2]),
                                        przejsciePrzod[przejsciePrzod.length-1][3])){
                            ++licznikPrawidlowychOdpowiedzi;
                        }
                        break;
                    case "Slight-Right-Turn":
                        oczekiwaneSygnaly = slightRight;
                        if(     przejsciePrzod[przejsciePrzod.length-1][2] >
                                Math.max(Math.max(przejsciePrzod[przejsciePrzod.length-1][0],
                                        przejsciePrzod[przejsciePrzod.length-1][1]),
                                        przejsciePrzod[przejsciePrzod.length-1][3])){
                            ++licznikPrawidlowychOdpowiedzi;
                        }
                        break;
                    case "Sharp-Right-Turn":
                        oczekiwaneSygnaly = sharpRight;
                        if(     przejsciePrzod[przejsciePrzod.length-1][3] >
                                Math.max(Math.max(przejsciePrzod[przejsciePrzod.length-1][1],
                                        przejsciePrzod[przejsciePrzod.length-1][2]),
                                        przejsciePrzod[przejsciePrzod.length-1][0])){
                            ++licznikPrawidlowychOdpowiedzi;
                        }
                        break;
                }

                for (int i = 0; i < iloscNeuronow[iloscNeuronow.length - 1]; ++i) {
                    funkcjaBledu += (Math.pow((oczekiwaneSygnaly[i] - przejsciePrzod[przejsciePrzod.length - 1][i]),2))/2;
                }


            }
            System.out.println(funkcjaBledu);
            System.out.println((double)licznikPrawidlowychOdpowiedzi / (iloscLinii - learningLines));
        }
    }
}
