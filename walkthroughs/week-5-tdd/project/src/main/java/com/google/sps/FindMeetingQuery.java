// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Math;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public final class FindMeetingQuery {

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    
    // Edge case if request's duration is over a day -> returns no options
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()){
        return Arrays.asList();
    }
    // Get all mandatory meeting attendees & optional attendees
    Set<String> mandatoryAttendees = new HashSet<>(request.getAttendees()); 
    Set<String> optionalAttendees = new HashSet<>(request.getOptionalAttendees());
    
    // Process which events are mandatory to get required #
    ArrayList<TimeRange> mandatoryEvents = new ArrayList<>();
    for (Event i: events) {
        if (mandatoryAttendees.containsAll(i.getAttendees())) {
            mandatoryEvents.add(i.getWhen());
        }
    }

    if (!(mandatoryAttendees.isEmpty()) && optionalAttendees.isEmpty()) {
        return getTimeSlotWithMandatoryAttendeesOnly(mandatoryEvents, request, TimeRange.START_OF_DAY, TimeRange.END_OF_DAY);
    } 
    else if ((mandatoryAttendees.isEmpty() && optionalAttendees.isEmpty())) { 
        return fullDayAsOpenSlot();
    } else {
        return getTimeSlotWithMostOptionalAttendees(events, request, mandatoryAttendees, optionalAttendees, mandatoryEvents.size());
    }
  }

  public Collection<TimeRange> getTimeSlotWithMostOptionalAttendees(Collection<Event> events, MeetingRequest request, 
        Set<String> mandatoryAttendees, Set<String> optionalAttendees, int numOfMandatoryEvents){
    
      // Generate all possible intervals in O(1) time
      int num = 60*24;
      int meetingDuration = (int) request.getDuration();
      ArrayList<TimeRange> possibleIntervals = new ArrayList<>();
      for (int currentMinute = 0; currentMinute < num - meetingDuration; currentMinute++) {
          possibleIntervals.add(TimeRange.fromStartEnd(currentMinute, currentMinute + meetingDuration, false));
      }
      
      // Maintain a frequency of timeSlot that already works for all mandatory attendee
      // to the # of optional attendees
      Map<TimeRange, Integer> frequency = new HashMap<>();

      // Maintain pointers
      int startTime = 0;
      int optimalEndTime = 0;
      int prevOptionalAttendee = 0;
      int prevMandatoryAttendee = 0;

      // Check all possible intervals and return the optimal one with the most optional attendees
      for (int j = 0; j < possibleIntervals.size(); j++) {
        TimeRange curInterval = possibleIntervals.get(j);
        int curMandatoryAttendee = 0;
        int curOptionalAttendee = 0;
        second:
        for (Event i : events) {
            // events do not overlap
            if (!curInterval.overlaps(i.getWhen())) {
                if (optionalAttendees.containsAll(i.getAttendees())) {
                    curOptionalAttendee++;
                } else if (mandatoryAttendees.containsAll(i.getAttendees())) {
                    curMandatoryAttendee++;
                }
            } else {
                // events overlap
                if (mandatoryAttendees.containsAll(i.getAttendees())) {
                    break second;
                }
            }
        }

        // Initializes variables for first event
        if (j == 0) {
            startTime = curInterval.start();
            optimalEndTime = curInterval.end();
            prevOptionalAttendee = curOptionalAttendee;
            prevMandatoryAttendee = curMandatoryAttendee;
            continue;
        }

        // In order for overlapping intervals to be extended, previous and current intervals
        // can fit all mandatory attendees and have the same number of optional attendee
        if (curMandatoryAttendee == numOfMandatoryEvents && curOptionalAttendee == prevOptionalAttendee) {

            // Check if this interval can be extended from the previous one
            if (prevMandatoryAttendee == numOfMandatoryEvents){
                // Can merge with previous interval
                optimalEndTime = curInterval.end();

            } else {
                // Not mergeable, must create a new interval
                startTime = curInterval.start();
                optimalEndTime = curInterval.end();
            }
        } else {
            // Check if the previous interval can be added 
            if (prevMandatoryAttendee == numOfMandatoryEvents) {
                TimeRange slot = TimeRange.fromStartEnd(startTime, optimalEndTime, false);
                frequency.put(slot, prevOptionalAttendee);
                
            }
            startTime = curInterval.start();
            optimalEndTime = curInterval.end();
        }

        prevMandatoryAttendee = curMandatoryAttendee;
        prevOptionalAttendee = curOptionalAttendee;

        if (j == possibleIntervals.size() - 1) {
            // Check if this last interval can be extended from the previous one
            if (prevMandatoryAttendee == numOfMandatoryEvents) {
                TimeRange slot = TimeRange.fromStartEnd(startTime, optimalEndTime, true);
                frequency.put(slot, prevOptionalAttendee);
            }
        }
      }
      
      // Return a collection of times with the maximum number of optional attendees
      return getKeysWithMaximumValue(frequency);
  }

  public Collection<TimeRange> getTimeSlotWithMandatoryAttendeesOnly(ArrayList<TimeRange> times, MeetingRequest request, int startTime, int endTime){
      
    ArrayList<TimeRange> openTime = new ArrayList<>();

    // Edge case if there are no conflicting events
    if (times.isEmpty()) {
        return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    // Sort all event by ascending start times O(nlogn)
    times.sort(TimeRange.ORDER_BY_START);

    // Create dummy "start of day" & "end of day" events
    times.add(0, TimeRange.fromStartEnd(startTime, startTime, false));
    times.add(TimeRange.fromStartEnd(endTime, endTime, false));

    // Maintain a pointer for optimal ending points
    TimeRange prevEvent = times.get(0);
    int optimalEndTime = prevEvent.end();

    for (int i = 1; i < times.size(); i++){
        TimeRange nextEvent = times.get(i);

        // Move on from overlapping events
        if (prevEvent.overlaps(nextEvent)) {
            optimalEndTime = Math.max(prevEvent.end(), nextEvent.end());
            prevEvent = nextEvent;
            continue;
        }

        // Non-overlapping events
        int duration = nextEvent.start() - optimalEndTime;
        if (duration >= request.getDuration()){
            // Deals with inclusivity if this is the last event
            if (i == times.size() - 1) {
                openTime.add(TimeRange.fromStartEnd(optimalEndTime, nextEvent.start(), true));
            } else {
                openTime.add(TimeRange.fromStartEnd(optimalEndTime, nextEvent.start(), false));
            }
        }

        optimalEndTime = nextEvent.end();
        prevEvent = nextEvent;
    }

    return openTime;
  }

  public Collection<TimeRange> fullDayAsOpenSlot(){
      return Arrays.asList(TimeRange.WHOLE_DAY);
  }

  public Collection<TimeRange> getKeysWithMaximumValue(Map<TimeRange, Integer> frequency){

    // Return the time slot with the max # of mandatory & optional attendees
    Map.Entry<TimeRange, Integer> maxEntry = null;
    for (Map.Entry<TimeRange, Integer> entry : frequency.entrySet()) {
        if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
            maxEntry = entry;
        }
    }
    Integer maxAttendees = maxEntry.getValue();

    // Run through frequency table to collect timeslots with the maximum attendees
    Collection<TimeRange> timeSlots = new ArrayList<>();
    for (Map.Entry<TimeRange, Integer> entry : frequency.entrySet()) {
        if (entry.getValue() ==  maxAttendees) {
            timeSlots.add(entry.getKey());
        }
    }
    return timeSlots;
  }
}
