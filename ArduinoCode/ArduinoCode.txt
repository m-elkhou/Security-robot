#include <NewPing.h>               // the Ultrasonic Library

#define down 11
#define up  10
#define left 12
#define right 13 //
int vitesse = 255;

#define trigLeft 6// 3l chmal
#define echoLeft 7
NewPing sonar(trigLeft, echoLeft, 200);
#define trigRigth 5// 3l lyman
#define echoRigth 4
NewPing sonar2(trigRigth, echoRigth, 200);
#define trig 17// fl wast
#define echo 18
NewPing sonar3(trig, echo, 200);

#define Movement 19//8

//#define buzzer 9
int buzzer = 9;

#define Pil 15
#define Pil1 2
#define Pil2 3
#define Pil3 16
#define Pil4 17

boolean upStat = false;
boolean downStat = false;
boolean leftStat = false;
boolean rightStat = false;
boolean isAutomaticStatus = true;
boolean gazStat = false;
boolean mStat = false;

void upStart(void) {
  analogWrite(down, 0);
  downStat = false;
  analogWrite(up, vitesse);
  upStat = true;
}

void downStart(void) {
  analogWrite(up, 0);
  upStat = false;
  analogWrite(down, vitesse);
  downStat = true;
}
void up_down_stop(void) {
  analogWrite(up, 0);
  analogWrite(down, 0);
  upStat = downStat = false;
}
void leftStart(void) {
  analogWrite(right, 0);
  analogWrite(left, 255);
  digitalWrite(Pil2, HIGH);
  leftStat = true;
  rightStat = false;
}

void rightStart(void) {
  analogWrite(left, 0);
  analogWrite(right, 255);
  digitalWrite(Pil1, HIGH);
  leftStat = false;
  rightStat = true;
}
void left_right_stop(void) {
  analogWrite(right, 0);
  analogWrite(left, 0);
  digitalWrite(Pil1, LOW);
  digitalWrite(Pil2, LOW);
  leftStat = rightStat = false;
}
void allStop(void) {
  analogWrite(up, 0);
  analogWrite(down, 0);
  analogWrite(left, 0);
  analogWrite(right, 0);

  digitalWrite(Pil1, LOW);
  digitalWrite(Pil2, LOW);
  upStat = downStat = rightStat = leftStat = false;
}

void StartAlarm(void) {
  digitalWrite(Pil, HIGH);
  digitalWrite(Pil1, HIGH);
  digitalWrite(Pil2, HIGH);
  digitalWrite(Pil3, HIGH);
  digitalWrite(Pil4, HIGH);
  digitalWrite(buzzer, HIGH);
  //          tone(buzzer, 1000, 200);
}

void StopAlarm(void) {
  digitalWrite(Pil, LOW);
  digitalWrite(Pil1, LOW);
  digitalWrite(Pil2, LOW);
  digitalWrite(Pil3, LOW);
  digitalWrite(Pil4, LOW);
  digitalWrite(buzzer, LOW);
  //      noTone(buzzer);
}

void setup() {
  Serial.begin(9600);// Open serial monitor at 9600 baud to see ping results.
  pinMode(down, OUTPUT);
  pinMode(up, OUTPUT);
  pinMode(left, OUTPUT);
  pinMode(right, OUTPUT);

  pinMode(Movement, INPUT);

  pinMode(Pil, OUTPUT);
  pinMode(Pil1, OUTPUT);
  pinMode(Pil2, OUTPUT);
  pinMode(Pil3, OUTPUT);
  pinMode(Pil4, OUTPUT);

  pinMode(buzzer, OUTPUT);

  digitalWrite(up, LOW);
  digitalWrite(down, LOW);
  digitalWrite(left, LOW);
  digitalWrite(right, LOW);

  digitalWrite(Pil, LOW);
  digitalWrite(Pil1, LOW);
  digitalWrite(Pil2, LOW);
  digitalWrite(Pil3, LOW);
  digitalWrite(Pil4, LOW);

  //  Serial.write("C");
}

void loop() {
  if ( isAutomaticStatus ) {
    upStart();
  }

  // Ultara Sonic Obstacle
  unsigned int uS = sonar.ping();
  int dLeft = uS / US_ROUNDTRIP_CM;
  uS = sonar2.ping();
  int dRight = uS / US_ROUNDTRIP_CM;
  uS = sonar3.ping();
  int d = uS / US_ROUNDTRIP_CM;

  if ( isAutomaticStatus ) {
    if (dRight <= 30 && dRight > 0 && (dLeft > 30 || dLeft <= 0)) {
      if ( !rightStat && !downStat && upStat ) {
        rightStart();
        downStart();
        delay(800);
        allStop();
        if ( isAutomaticStatus ) {
          leftStart();
          upStart();
          delay(300);
          left_right_stop();
        }
      }
    } else if (dLeft <= 30 && dLeft > 0 && (dRight > 30 || dRight <= 0)) {
      if ( !leftStat && !downStat && upStat ) {
        leftStart();
        downStart();
        delay(800);
        allStop();
        if ( isAutomaticStatus ) {
          rightStart();
          upStart();
          delay(300);
          left_right_stop();
        }
      }
    } else if ( dLeft <= 30 && dLeft > 0 && dRight <= 30 && dRight > 0 ) {
      if (dLeft >= dRight) {
        if ( !rightStat && !downStat && upStat ) {
          leftStart();
          downStart();
          delay(700);
          allStop();
          if ( isAutomaticStatus ) {
            rightStart();
            upStart();
            delay(300);
            left_right_stop();
          }
        }
      } else {
        if ( !leftStat && !downStat && upStat ) {
          leftStart();
          downStart();
          delay(1000);
          allStop();
          if ( isAutomaticStatus )
            rightStart();
          upStart();
          delay(300);
          left_right_stop();
        }
      }
    } else if (dLeft <= 60 && dLeft > 0 && (dRight > 60 || dRight <= 0)) {
      if (!rightStat && upStat ) {
        rightStart();
        delay(500);
        left_right_stop();
      }
    } else if (dRight <= 60 && dRight > 0 && (dLeft > 60 || dLeft <= 0)) {
      if (!leftStat && upStat ) {
        leftStart();
        delay(500);
        left_right_stop();
      }
    } else if (dLeft <= 60 && dLeft > 0 && dRight <= 60 && dRight > 0) {
      if (dLeft >= dRight) {
        if (!leftStat && upStat ) {
          leftStart();
          delay(500);
          left_right_stop();
        }
      } else {
        if (!rightStat && upStat ) {
          rightStart();
          delay(500);
          left_right_stop();
        }
      }
    }
  }
  //  Gaz
  int gaz = 0;
  gaz = analogRead(0);
  if (!gazStat && (gaz == 180 || gaz > 400)) {
    Serial.write('F');
    //    delay(500);
  }

  //  Movemont
//      int dr = 0;
//      dr = digitalRead(Movement);  // read input value
//      if ( dr == HIGH) {            // check if the input is HIGH
//        Serial.write('M');
//        delay(1000);
//      }


  /////// cheque comande from USB
  if (Serial.available()) {
    int cmd = Serial.read();
    switch (cmd) {
      case 48: // 0 - is Auto
        if (isAutomaticStatus) {
          isAutomaticStatus = false;
          allStop();
        }
        else {
          isAutomaticStatus = true;
          upStart();
        }
        Serial.write('A');
        break;
      case 49: // 1 - up start
        upStart();
        break;
      case 50: // 2 - up/down stop
        up_down_stop();
        break;
      case 51: // 3 - down start
        downStart();
        break;
      case 52: // 4 - left start
        leftStart();
        break;
      case 53: // 5 - left/right stop
        left_right_stop();
        break;
      case 54: // 6 - right start
        rightStart();
        break;
      case 55: // 7 - set speed
        vitesse = Serial.read();
        break;
      case 57: // 9 - all stop
        allStop();
        break;
      case 65: // A

        break;
      case 72:
        gazStat = false;
        break;
    }
  }
  delay(50);
}
