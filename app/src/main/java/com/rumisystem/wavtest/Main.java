package com.rumisystem.wavtest;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;
import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;

public class Main {
	public static final File CDDA_DIR = new File("/run/user/1000/gvfs/cdda:host=sr0");
	public static int TRACK_NUM = 0;
	public static int TRACK_MAX = 0;
	public static List<byte[]> PCM_DATA_LIST = new ArrayList<>();

	public static void main(String[] args) {
		try{
			//CDが入るまで待機する
			while (true) {
				//CDがあるか
				if (CDDA_DIR.exists() && CDDA_DIR.isDirectory()) {
					LOG(LOG_TYPE.OK, "CDを検出");
					break;
				}
			}

			File[] TRACK_LIST = CDDA_DIR.listFiles();
			TRACK_MAX = TRACK_LIST.length;
			LOG(LOG_TYPE.INFO, TRACK_MAX + "トラックあります");

			//どのトラックを選ぶか
			while (true) {
				System.out.print(">");
				int SELECT_TRACK = Integer.parseInt(new Scanner(System.in).nextLine());
				if (SELECT_TRACK <= TRACK_LIST.length && SELECT_TRACK >= 1) {
					TRACK_NUM = SELECT_TRACK;
					break;
				}
			}

			//再生する
			LOG(LOG_TYPE.INFO, "トラック" + TRACK_NUM + "を再生します");
			PLAY_WAV();
		}catch(Exception EX){
			EX.printStackTrace();
		}
	}

	public static void PLAY_WAV() throws Exception{
		byte[] DATA = Files.readAllBytes(Paths.get(CDDA_DIR.getPath(), "Track " + TRACK_NUM + ".wav"));
		int CHANEL = 1;
		int SAMPLING_RATE = 44100;
		int BLOCK_SIZE = 0;

		LOG(LOG_TYPE.OK, "ファイルを開いた");

		if(HEX(DATA[0]).equals("52") && HEX(DATA[1]).equals("49") && HEX(DATA[2]).equals("46") && HEX(DATA[3]).equals("46")){
			LOG(LOG_TYPE.OK, "WAVファイルを検知");
			for(int I = 0; I < DATA.length; I++){
				//HEXで「fmt 」の並びを検出するだけ
				if(HEX(DATA[I]).equals("66") && HEX(DATA[I + 1]).equals("6D") && HEX(DATA[I + 2]).equals("74") && HEX(DATA[I + 3]).equals("20")){
					int POS = I + 4;

					LOG(LOG_TYPE.OK, "fmtセクションを検出");

					//4バイトぐらいfmtのサイズが書かれているけど不要なのでスキップ
					POS += 4;

					//フォーマット形式がPCMで有ることをチェック
					if(!HEX(DATA[POS]).equals("01")){
						LOG(LOG_TYPE.FAILED, "フォーマットはPCMである必要があります！！");
						System.exit(1);
					}

					//2バイト進める
					POS += 2;

					//チャンネル数
					CHANEL = Integer.parseInt(HEX(DATA[POS]));

					//2バイト進める
					POS += 2;

					//サンプリングレート(なぜか逆から読むらしい)
					SAMPLING_RATE = (int)Long.parseLong(HEX(DATA[POS + 1]) + HEX(DATA[POS]), 16);

					//ブロックサイズ
					POS += 8;
					BLOCK_SIZE = (int)Long.parseLong(HEX(DATA[POS + 1]) + HEX(DATA[POS]), 16);
				}

				//HEXでdataの並びを検出するだけ
				if(HEX(DATA[I]).equals("64") && HEX(DATA[I + 1]).equals("61") && HEX(DATA[I + 2]).equals("74") && HEX(DATA[I + 3]).equals("61")){
					int POS = I + 4;

					LOG(LOG_TYPE.OK, "dataセクションを検出");

					long DATA_LENGTH = Long.parseLong(HEX(DATA[POS + 3]) + HEX(DATA[POS + 2]) + HEX(DATA[POS + 1]) + HEX(DATA[POS]), 16);

					//情報
					LOG(LOG_TYPE.INFO, "-------------------------------------------------");
					LOG(LOG_TYPE.INFO, "フォーマット：PCM");
					LOG(LOG_TYPE.INFO, "チャンネル数：" + CHANEL);
					LOG(LOG_TYPE.INFO, "サンプリングレート：" + SAMPLING_RATE);
					LOG(LOG_TYPE.INFO, "データサイズ：" + DATA_LENGTH);
					LOG(LOG_TYPE.INFO, "曲の長さ：" + GetSongLength(DATA_LENGTH, SAMPLING_RATE, BLOCK_SIZE));
					LOG(LOG_TYPE.INFO, "-------------------------------------------------");

					int sampleSizeInBits = 16;
					boolean signed = true;
					boolean bigEndian = false;
					int BUFFER_SIZE = (SAMPLING_RATE * CHANEL * BLOCK_SIZE * 1);
					AudioFormat format = new AudioFormat(SAMPLING_RATE, sampleSizeInBits, CHANEL, signed, bigEndian);


					SourceDataLine line = AudioSystem.getSourceDataLine(format);
					line.open(format);
					line.start();

					LOG(LOG_TYPE.OK, "スピーカーを初期化しました、再生を開始します");
					System.out.println("");

					for(int P = POS + 4; P < DATA_LENGTH; P += BUFFER_SIZE){
						PROGRESS_DRAW(P, (int) DATA_LENGTH, SAMPLING_RATE, BLOCK_SIZE);

						//読み込んだ分を入れる
						line.write(DATA, 0, BUFFER_SIZE);
					}

					LOG(LOG_TYPE.OK, "再生完了");

					//開放
					line.close();
					break;
				}
			}
		} else {
			LOG(LOG_TYPE.FAILED, "非対応のファイルを検知");
			System.exit(1);
		}
	}

	public static String HEX(byte BYTE){
		//a
		return String.format("%02X", BYTE);
	}

	public static double GetSongLength(long DATA_LENGTH, int SAMPLING_RATE, int BLOCK_SIZE) {
		return (double) (DATA_LENGTH / BLOCK_SIZE) / SAMPLING_RATE;
	}

	//秒数を分秒に
	public static String SecToMinSec(double IN) {
		int H = (int) Math.floor(IN / 3600);
		int M = (int) Math.floor(IN / 60);
		int S = (int) (IN % 60);

		return String.format("%02d", H) + ":" + String.format("%02d", M) + "." + String.format("%02d", S);
	}

	public static void PROGRESS_DRAW(int NOW, int MAX, int SAMPLING_RATE, int BLOCK_SIZE) {
		int SIZE = 60;
		int PROGRESS = (int) (((double) NOW / MAX) * SIZE);
		double PLAY_SEC = GetSongLength(NOW, SAMPLING_RATE, BLOCK_SIZE);
		StringBuilder PROGRESS_TEXT = new StringBuilder();

		for (int I = 0; I < PROGRESS; I++) {
			PROGRESS_TEXT.append("=");
		}

		for (int I = PROGRESS; I < SIZE; I++) {
			PROGRESS_TEXT.append(" ");
		}

		System.out.println("\033[1F" + SecToMinSec(PLAY_SEC) + "[" + PROGRESS_TEXT + "]");
	}
}
