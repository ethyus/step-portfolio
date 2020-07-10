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
    // Get all mandatory meeting attendees
    Set<String> attendees = new HashSet<>(request.getAttendees()); 

    // Get all optional attendees
    Set<String> optAttendees = new HashSet<>(request.getOptionalAttendees());
    
    // Process which events are from mandatory or optional attendees
    ArrayList<TimeRange> mandatoryEvents = new ArrayList<>();
    ArrayList<TimeRange> optEvents =  new ArrayList<>();
    HashMap<String, ArrayList<TimeRange>> optAttendeesEvents = new HashMap<>();

    for (String i: optAttendees) {
        optAttendeesEvents.put(i, new ArrayList<TimeRange>());
    }

    for (Event i: events) {
        if (optAttendees.containsAll(i.getAttendees())) {
            optEvents.add(i.getWhen());
        } else if (attendees.containsAll(i.getAttendees())) {
            mandatoryEvents.add(i.getWhen());
        }
    }

    // request has only mandatory attendees so return time slots for them
    if (attendees.isEmpty() && !(optAttendees.isEmpty())) {
        return driver(optEvents, request);
    } // request has no mandatory attendees so return time slots for optional attendees only
    else if (!(attendees.isEmpty()) && optAttendees.isEmpty()) { 
        return driver(mandatoryEvents, request);
    } // request has no attendees so return whole day as available
    else if ((attendees.isEmpty() && optAttendees.isEmpty())) { 
        return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    // See if it's possible to optimize timeslots with mandatory & optional attendees
    // Maintain a frequency table of timeslot to available optional attendees

    Map<TimeRange, Integer> frequency = new HashMap<>();
    Collection<TimeRange> availableTimeSlots = driver(mandatoryEvents, request);

    for (TimeRange i: availableTimeSlots) {
      for (TimeRange j: optEvents) {
        if (i.overlaps(j)) {
            continue;
        } else {
            if (frequency.containsKey(i)) {
                frequency.merge(i, 1, (a,b) -> a + b);
            } else {
                frequency.put(i, 1); 
            }
        }
      }
    }
    // Check to see if all optional attendees can attend 
    Set<Integer> attendeeNumbers = new HashSet<> (frequency.values());
  
    // Optimize
    if (frequency.size() >= 1) {
        if (attendeeNumbers.size() == 1 && attendeeNumbers.contains(optEvents.size())) {
            // All optional attendees can attend 
            return new ArrayList<TimeRange>(frequency.keySet());
        }
        else {
            // Not all optional attendees can attend, try to optimize
            return getOptimizedTimeSlot(frequency);
        }
    } 

    return availableTimeSlots;
  }

  public Collection<TimeRange> getOptimizedTimeSlot(Map<TimeRange, Integer> frequency){

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
  
  public Collection<TimeRange> driver(ArrayList<TimeRange> listOfTime, MeetingRequest request){
    // Main logic of algorithm

    ArrayList<TimeRange> openTime = new ArrayList<>();

    // Edge case if there are no conflicting events
    if (listOfTime.isEmpty()) {
        return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    // Sort all event by ascending start times O(nlogn)
    listOfTime.sort(TimeRange.ORDER_BY_START);

    // Maintain a pointer for local max of ending points
    TimeRange prevEvent = listOfTime.get(0);
    int localMax = prevEvent.end();

    // Check for potential open interval from start of day to first event
    if (prevEvent.start() > TimeRange.START_OF_DAY){
        int interval = prevEvent.start() - TimeRange.START_OF_DAY;
        if (interval >= request.getDuration()){
            openTime.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, prevEvent.start(), false));
            localMax = prevEvent.end();
        }
    }

    for (int i = 1; i < listOfTime.size(); i++){
        TimeRange nextEvent = listOfTime.get(i);

        // Move on from overlapping events
        // 1st case considers nested events
        if (prevEvent.overlaps(nextEvent) && localMax >= nextEvent.end()){
            prevEvent = listOfTime.get(i);
            continue;
        } else if (prevEvent.overlaps(nextEvent)) {
            localMax = nextEvent.end();
            prevEvent = listOfTime.get(i);
            continue;
        }

        // Non-overlapping events
        int openInterval = nextEvent.start() - localMax;
        if (openInterval >= request.getDuration()){
            openTime.add(TimeRange.fromStartEnd(localMax, nextEvent.start(), false));
        }

        localMax = Math.max(localMax, nextEvent.end());
        prevEvent = listOfTime.get(i);
    }
    
    // Check for potential interval from last event to end of day
    if (localMax < TimeRange.END_OF_DAY){
        int interval = TimeRange.END_OF_DAY - localMax;
        if (interval >= request.getDuration()){
            openTime.add(TimeRange.fromStartEnd(localMax, TimeRange.END_OF_DAY, true));
        }
    }

    return openTime;
  }
}
