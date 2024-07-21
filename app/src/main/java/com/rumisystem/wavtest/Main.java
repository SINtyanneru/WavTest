package com.rumisystem.wavtest;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;
import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;

public class Main {
	public static final String WAV_FILE_PATH = "/mnt/DATA_BOX/MP3/F03/Enya/Paint The Sky With Stars- The Best Of Enya/03 Book Of Days.wav";

	public static void main(String[] args) {
		try{
			byte[] FILE_DATA = Files.readAllBytes(Paths.get(WAV_FILE_PATH));
			int CHANEL = 1;
			int SAMPLING_RATE = 44100;

			LOG(LOG_TYPE.OK, "ファイルを開いた");
			
			if(HEX(FILE_DATA[0]).equals("52") && HEX(FILE_DATA[1]).equals("49") && HEX(FILE_DATA[2]).equals("46") && HEX(FILE_DATA[3]).equals("46")){
				LOG(LOG_TYPE.OK, "WAVファイルを検知");
				for(int I = 0; I < FILE_DATA.length; I++){
					//HEXで「fmt 」の並びを検出するだけ
					if(HEX(FILE_DATA[I]).equals("66") && HEX(FILE_DATA[I + 1]).equals("6D") && HEX(FILE_DATA[I + 2]).equals("74") && HEX(FILE_DATA[I + 3]).equals("20")){
						int POS = I + 4;

						LOG(LOG_TYPE.OK, "fmtセクションを検出！位置「" + POS + "」だ！");

						//罫線
						LOG(LOG_TYPE.INFO, "-------------------------------------------------");

						//4バイトぐらいfmtのサイズが書かれているけど不要なのでスキップ
						POS += 4;

						//フォーマット形式がPCMで有ることをチェック
						if(!HEX(FILE_DATA[POS]).equals("01")){
							LOG(LOG_TYPE.FAILED, "フォーマットはPCMである必要があります！！");
							System.exit(1);
						} else {
							LOG(LOG_TYPE.INFO, "フォーマット：PCM");
						}

						//2バイト進める
						POS += 2;

						//チャンネル数
						CHANEL = Integer.parseInt(HEX(FILE_DATA[POS]));
						LOG(LOG_TYPE.INFO, "チャンネル数：" + CHANEL);

						//2バイト進める
						POS += 2;

						//サンプリングレート(なぜか逆から読むらしい)
						SAMPLING_RATE = (int)Long.parseLong(HEX(FILE_DATA[POS + 1]) + HEX(FILE_DATA[POS]), 16);
						LOG(LOG_TYPE.INFO, "サンプリングレート：" + SAMPLING_RATE);

						//罫線
						LOG(LOG_TYPE.INFO, "-------------------------------------------------");
					}

					//HEXでdataの並びを検出するだけ
					if(HEX(FILE_DATA[I]).equals("64") && HEX(FILE_DATA[I + 1]).equals("61") && HEX(FILE_DATA[I + 2]).equals("74") && HEX(FILE_DATA[I + 3]).equals("61")){
						int POS = I + 4;

						LOG(LOG_TYPE.OK, "dataセクションを検出！位置「" + POS + "」だ！");

						long DATA_LENGTH = Long.parseLong(HEX(FILE_DATA[POS]) + HEX(FILE_DATA[POS + 1]) + HEX(FILE_DATA[POS + 2]) + HEX(FILE_DATA[POS + 3]), 16);

						LOG(LOG_TYPE.INFO, "データサイズ：" + DATA_LENGTH);

						//ファイルと位置を渡す、位置はIだとdataのdから始まるので+4する
						PLAY_WAV(FILE_DATA, POS, DATA_LENGTH, CHANEL, SAMPLING_RATE);
						break;
					}
				}
			} else {
				LOG(LOG_TYPE.FAILED, "非対応のファイルを検知");
				System.exit(1);
			}

		}catch(Exception EX){
			EX.printStackTrace();
		}
	}

	public static void PLAY_WAV(byte[] DATA, int POS, long DATA_LENGTH, int CHANEL, int SAMPLING_RATE) throws Exception{
		int sampleSizeInBits = 16;
		boolean signed = true;
		boolean bigEndian = false;
		AudioFormat format = new AudioFormat(SAMPLING_RATE, sampleSizeInBits, CHANEL, signed, bigEndian);

		SourceDataLine line = AudioSystem.getSourceDataLine(format);
		line.open(format);
		line.start();

		LOG(LOG_TYPE.OK, "スピーカーを初期化しました、再生を開始します");

		for(int I = POS + 4; I < DATA_LENGTH; I += 1000){
			byte[] BUFFER = new byte[1000];

			//1000バイトずつ読み込む
			for(int I2 = 0; I2 < BUFFER.length; I2++){
				BUFFER[I2] = DATA[I + I2];
			}

			//読み込んだ分を入れる
			line.write(BUFFER, 0, BUFFER.length);
			//line.drain();
		}

		LOG(LOG_TYPE.OK, "再生完了");

		//開放
		line.close();

		LOG(LOG_TYPE.OK, "開放しました");
	}

	public static String HEX(byte BYTE){
		return String.format("%02X", BYTE);
	}
}
