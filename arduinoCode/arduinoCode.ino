const int NUM_LEDS = 8;
const int ledPins[NUM_LEDS] = {4, 5, 6, 7, 8, 9, 10, 11};

void setup() {
  Serial.begin(9600);
  
  for(int i = 0; i < NUM_LEDS; i++) {
    pinMode(ledPins[i], OUTPUT);
    digitalWrite(ledPins[i], LOW);
  }
}

void loop() {
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    
    int separatorIndex = command.indexOf(':');
    if(separatorIndex != -1) {
      String cmd = command.substring(0, separatorIndex);
      int ledIndex = command.substring(separatorIndex + 1).toInt();
      
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
    Serial.flush();
  }
}