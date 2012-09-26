/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.phono.jingle.test;

import com.phono.api.faces.PhonoCallFace;
import com.phono.api.faces.PhonoNativeFace;
import com.phono.api.faces.PhonoPhoneFace;
import com.phono.api.faces.PhonoMessageFace;
import com.phono.jingle.PhonoNative;
import com.phono.jingle.PhonoPhone;
import com.phono.jingle.PhonoCall;
import com.phono.jingle.PhonoMessaging;

import com.phono.srtplight.Log;
import java.io.IOException;
import java.util.Hashtable;


/**
 *
 * @author tim
 */
public class CommandLineClient {

    PhonoNativeFace _pn;
    PhonoPhoneFace _phone;
    PhonoCallFace _call;
    Thread _console;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Log.setLevel(0);
        CommandLineClient clc = new CommandLineClient();
    }

    CommandLineClient() {
        Log.debug("Starting command line client");
        final PhonoPhone phone = new PhonoPhone() {

            @Override
            public PhonoCall newCall() {
                return new PhonoCall(this) {

                    @Override
                    public void onRing() {
                        System.out.println("Phone ringing ");
                    }

                    @Override
                    public void onAnswer() {
                        System.out.println("Phone answered ");
                    }

                    @Override
                    public void onHangup() {
                        System.out.println("Phone hungup ");
                        _call = null;
                    }

                    @Override
                    public void onError() {
                        System.out.println("Phone error ");
                        _call = null;

                    }
                };
            }

            @Override
            public void onIncommingCall(PhonoCallFace c) {
                _call = c;
                System.out.println("Press a<ret> to answer");
            }
        };
        final PhonoMessaging messing = new PhonoMessaging() {
            @Override
            public void onMessage(PhonoMessageFace message) {
                System.out.println("message from " + message.getFrom() + ": " + message.getBody());
            }
        };
        //final Runnable cli = this;
        _pn = new PhonoNative() {

            @Override
            public void onReady() {
                System.out.println("Connection Ready");
                if (_console == null) {
                    Runnable cli = new Runnable() {
                        public void run() {
                            consoleUI();
                        }
                    };
                    _console = new Thread(cli);
                    _console.start();
                }
            }

            @Override
            public void onUnready() {
                _console = null; // thread will die soonish
            }

            @Override
            public void onError() {
                System.out.println("Connection Error! ");
            }
        };
        _pn.setPhone(phone);
        _pn.setMessaging(messing);
        _pn.connect();
        phone.setRingTone("http://s.phono.com/ringtones/Diggztone_Piano.mp3");
        phone.setRingbackTone("http://s.phono.com/ringtones/ringback-uk.mp3");
        _phone = phone;

    }

    private void consoleUI() {
        while (_console != null) {
            try {
                if (_call != null) {
                    System.out.println("Press h<ret> to hangup or 0-9*# <ret> to send dtmf");
                } else {
                    System.out.println("Press d<ret> to dial");
                }
                int c = System.in.read();

                if ((c == 'h') || (c == 'H')) {
                    if (_call != null) {
                        _call.hangup();
                        _call = null;
                    } else {
                        System.out.println("No current call to hangup on...");
                    }
                }
                if ((c == 'd') || (c == 'D')) {
                    if (_call == null) {
                        _call = _phone.dial("9996160714@app", null);
                    } else {
                        System.out.println("Currently in a call, can't start a new one.");
                    }
                }
                if ("0123456789*#".indexOf(c) >= 0) {
                    _call.digit(new Character((char) c));
                }
                if ((c == 'a') || (c == 'A')) {
                    _call.answer();
                }
            } catch (IOException ex) {
                Log.warn(ex.getMessage());
            }
        }

    }
}