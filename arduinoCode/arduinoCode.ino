#include <Servo.h>

const int NUM_LEDS = 8;
const int NUM_DOORS = 6;
const int ledPins[NUM_LEDS] = {4, 5, 6, 7, 8, 9, 10, 11};
const int servoPins[NUM_DOORS] = {2, 3, 12, 13, A0, A1};  // Pines para los servomotores

Servo doorServos[NUM_DOORS];  // Array de objetos Servo

// Posiciones para los servos
const int CLOSED_POSITION = 0;    // Posición cerrada
const int OPEN_POSITION = 90;     // Posición abierta (90 grados)

void setup() {
  Serial.begin(9600);
  
  // Configurar LEDs
  for(int i = 0; i < NUM_LEDS; i++) {
    pinMode(ledPins[i], OUTPUT);
    digitalWrite(ledPins[i], LOW);
  }

  // Configurar Servomotores
  for(int i = 0; i < NUM_DOORS; i++) {
    doorServos[i].attach(servoPins[i]);
    doorServos[i].write(CLOSED_POSITION);  // Iniciar todas las puertas cerradas
  }
}

void loop() {
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    
    int separatorIndex = command.indexOf(':');
    if(separatorIndex != -1) {
      String cmd = command.substring(0, separatorIndex);
      String param = command.substring(separatorIndex + 1);
      
      // Manejar comandos LED
      if(cmd == "LED_ON" || cmd == "LED_OFF") {
        handleLEDCommand(cmd, param.toInt());
      }
      // Manejar comandos de puerta
      else if(cmd == "DOOR_OPEN" || cmd == "DOOR_CLOSE") {
        handleDoorCommand(cmd, param);
      }
    }
    Serial.flush();
  }
}

void handleLEDCommand(String cmd, int ledIndex) {
  if(ledIndex >= 0 && ledIndex < NUM_LEDS) {
    if(cmd == "LED_ON") {
      digitalWrite(ledPins[ledIndex], HIGH);
      Serial.println("SUCCESS:LED_ON");
    }
    else if(cmd == "LED_OFF") {
      digitalWrite(ledPins[ledIndex], LOW);
      Serial.println("SUCCESS:LED_OFF");
    }
  }
}

void handleDoorCommand(String cmd, String doorName) {
  int doorIndex = getDoorIndex(doorName);
  
  if(doorIndex >= 0 && doorIndex < NUM_DOORS) {
    if(cmd == "DOOR_OPEN") {
      doorServos[doorIndex].write(OPEN_POSITION);
      Serial.println("SUCCESS:DOOR_OPEN:" + doorName);
    }
    else if(cmd == "DOOR_CLOSE") {
      doorServos[doorIndex].write(CLOSED_POSITION);
      Serial.println("SUCCESS:DOOR_CLOSE:" + doorName);
    }
  }
}

int getDoorIndex(String doorName) {
  if(doorName == "GARAGE") return 0;
  if(doorName == "MAIN") return 1;
  if(doorName == "KITCHEN") return 2;
  if(doorName == "LIVING") return 3;
  if(doorName == "BEDROOM1") return 4;
  if(doorName == "BEDROOM2") return 5;
  return -1;
}