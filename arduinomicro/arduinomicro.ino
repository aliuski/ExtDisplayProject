/* 
  This software send pressed button data to bluetooth module. Also it recept data to bluetooth device and show it to device.
*/

// HARMAJA00--KUMPULA00--VUOSAARI00--00:00

  const byte text1[] = {3,2, 4, 3,7,4, 2,7,4, 3,17,4, 2,5,4, 2,12,4, 3,9,4, 2,9,4, 2,17,4, 2,1,3,1,4, 2,3,4, 3,1,2,1,4, 3,20,
  3,10,4, 3,10,4, 2,8,4, 3,3,4, 3,5,4, 2,9,4, 2,11,4, 2,17,4, 2,1,3,1,4, 2,3,4, 3,1,2,1,4, 3,20,
  3,21,4, 2,1,4, 2,6,4, 3,4,4, 2,18,4, 2,1,3,1,4, 3,17,4, 2,9,4, 2,25,4, 2,1,3,1,4, 2,3,4, 3,1,2,1,4, 3,3,4,
  2,1,3,1,4, 3,10,4, 2,10,4, 2,1,3,1,4, 0};

  const byte text2[] = {3,2, 4, 4, 3,10, 4, 2,2, 4, 2,18, 4, 2,5, 4, 0};

  const int BUTTON_P_D = 2;    // P/D
  const int BUTTON_P_U = 3;    // P/U
  const int BUTTON_ON_OFF = 4; // ON/OFF

  int counter;
  int stepcounter;
  byte myProgram[sizeof(text1)];
  byte buffer[11];
  bool runningState;
  bool newWrite;
 
void setup() {
  Serial1.begin(115200);
  pinMode(BUTTON_P_D, OUTPUT);
  pinMode(BUTTON_P_U, OUTPUT);
  pinMode(BUTTON_ON_OFF, OUTPUT);

  pinMode(11, INPUT);
  pinMode(12, INPUT);

  counter = 0;
  stepcounter = 0;
  runningState = false;
  newWrite = false;
}

void loop() {

  if (Serial1.available()){
    Serial1.readBytes(buffer, 11);
    if(!runningState){
      memcpy(myProgram, text1, sizeof(text1));
      writeSpeed(myProgram, 24, buffer[0], buffer[1], buffer[2]);
      writeSpeed(myProgram, 63, buffer[3], buffer[4], buffer[5]);
      writeSpeed(myProgram, 107, buffer[6], buffer[7], buffer[8]);
      writeTime(myProgram, 123, buffer[9], buffer[10]);
    }
    runningState = true;
  }

  if(digitalRead(11) == HIGH){
    if(!newWrite){
      newWrite = true;
      Serial1.write(1);
    }
  }
  
  if(digitalRead(12) == HIGH){
    memcpy(myProgram, text2, sizeof(text2));
    
    runningState = true;
  }
  
  if(runningState){
    if(myProgram[counter] == BUTTON_ON_OFF){
      writePort(myProgram[counter], 1);
      if(stepcounter == 0)
        counter += 1;
    } else {
      writePort(myProgram[counter], myProgram[counter+1]);
      if(stepcounter == 0)
        counter += 2;
    }
    if(myProgram[counter] == 0){
      counter = 0;
      runningState = false;
      newWrite = false;
      digitalWrite(BUTTON_P_D, HIGH);
      digitalWrite(BUTTON_P_U, LOW);
      digitalWrite(BUTTON_ON_OFF, LOW);
      delay(1000);
      digitalWrite(BUTTON_P_D, LOW);
    }
  }
}

void writePort(int butt, int repeat){
    if(stepcounter % 2){
      digitalWrite(butt, HIGH);
      delay(10);
    } else {
      digitalWrite(BUTTON_P_D, LOW);
      digitalWrite(BUTTON_P_U, LOW);
      digitalWrite(BUTTON_ON_OFF, LOW);
      delay(20);
    }
    stepcounter++;
    if(stepcounter == (repeat * 2))
      stepcounter = 0;
}

void writeSpeed(byte *table, int index, byte speed, byte windchange ,byte windfuture){

  byte ten = speed/10;
  byte one = speed - ten*10;

  table[index+1] -= ten;
  table[index+4] += ten;
  table[index+6] += one;

  byte d = table[index+9] + one;
  byte e = table[index+12];
  if(windchange == '+'){
   d += 2;
   e += 2;
  } else if(windchange == '*'){
   d += 3;
   e += 3;
  }
  table[index+9] = d;
  table[index+12] = e;

  d = table[index+14];
  e = table[index+17];
  if(windfuture == '+'){
   d += 2;
   e += 2;
  } else if(windfuture == '*'){
   d += 3;
   e += 3;
  }
  table[index+14] = d;
  table[index+17] = e;
}

void writeTime(byte *table, int index, byte hour, byte minute){

  byte hten = hour/10;
  byte hone = hour - hten*10;
  byte mten = minute/10;
  byte mone = minute - mten*10;
  
  table[index+1] += hten;
  table[index+4] += hten;
  table[index+6] += hone;
  table[index+9] -= hone;
  
  table[index+12] -=  mten;
  table[index+15] +=  mten;
  table[index+17] +=  mone;
}

