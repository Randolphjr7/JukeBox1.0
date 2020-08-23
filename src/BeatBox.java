import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class BeatBox {

    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxlist;
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
                                "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell",
                                "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};

    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        new BeatBox().buildGUI();
    }

    public void buildGUI() {
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        // empty border gives us a margin between the edges of the panel and where the components are placed
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        checkboxlist = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        // buttons
        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setVgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        // make the checkBoxes, set them to 'false' (so they aren't checked) and add
        // them to the ArrayList AND to the GUI panel
        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxlist.add(c);
            mainPanel.add(c);
        } // end loop

        setUpMidi();

        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    } // close method

    // the usual MIDI set-up stuff for getting the Sequencer, the Sequence and the Track.
    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        } // close method
    }

    // where the action happens; were we turn checkbox state into MIDI evnets  & add them to the Track
    public void buildTrackAndStart() {
        /* make a 16-element array to hold the values for one instrument , across all 16 beats. If the
        *  instrument is supposed to play on that beat, the value at that element will be the key. If
        *  that instrument is NOT supposed to play on that beat put in a zero */
        int[] trackList = null;

        // get rid of the old track, make a fresh one
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        // do this for each of the 16 rows
        for (int i = 0; i <16; i++) {
            trackList = new int[16];

            // set the 'key' that represents which instrument this is (Bass, Hi-Hat, the instruments array holds
            // the actual MIDI numbers for each instrument
            int key = instruments[i];

            // do this for each of the BEATS for this row
            for (int j = 0; j <16; j++) {

                JCheckBox jc = checkboxlist.get(j + 16 * i);
                /* Is the checkbox at this beat selected? If yes, put the key value in this slot in the array
                *  (the slot that represents this beat) Otherwise, the instrument is NOT suppose to play at
                *  this beat, so set it to zero */
                if (jc.isSelected()) {
                    trackList[j] = key;
                } else {
                    trackList[j] = 0;
                }
            } // close inner loop

            // for this instrument, & for all 16 beats, make events & add them to the track
            makeTracks(trackList);
            track.add(makeEvent(176,1,127,0,16));
        } // close outer loop

        /* We also want to make sure that there is an event at beat 16 (it goes 0 to 15) Otherwise, the
        *  BeatBox might not go the full 16 beats before it starts over */
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // close buildTrackAndStart method

    // First of the inner classes, listeners for the buttons
    public class MyStartListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    } // close inner class

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    } // close inner class

    public class MyUpTempListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * 1.03));
        }
    } // close inner class

    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * .97));
        }
    } // close inner class

    /* Makes events for one instrument at a time, for all 16 beats. SO it might get an int[] for the BAss drum,
    * and each index in the array will hold either the key of that instument or a zero. If it's a zero, the
    * instrument isn't supposed to play at that beat. Otherwise make an event and add it to the track*/
    public void makeTracks(int[] list) {

        for (int i = 0; i < 16; i++) {
            int key = list[i];
            if (key != 0) {
                // make the NOTE ON and NOTE OFF events, & add them to the track
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(128, 9, key, 100, i + 1));
            }
        }
    } // end of makeTracks(int [] list) method

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }
} // close class
