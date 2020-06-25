// Type Writer JS Animation

class TyperWriter {
  constructor(elements, words, waitTime = 3000) {
    this.elements = elements;
    this.words = words;
    this.wordIndex = 0;
    this.txt = "";
    this.waitTime = parseInt(waitTime, 10); //Base 10
    this.typeFunc();
    this.isDelete = false;
  }
}
//typing main function
TyperWriter.prototype.typeFunc = function () {
  console.log(this.words.length);
  const current_index = this.wordIndex % this.words.length;
  const full_word = this.words[current_index];

  if (this.isDelete) {
    //Remove character
    this.txt = full_word.substring(0, this.txt.length - 1);
  } else {
    //Add character
    this.txt = full_word.substring(0, this.txt.length + 1);
  }

  //Output on website
  this.elements.innerHTML = '<span class = "text">' + this.txt + "</span>";
  let typeSpeed = 100;
  if (this.isDelete) {
    typeSpeed /= 2;
  }
  //Change type speed when deleting
  if (!this.isDelete && this.txt == full_word) {
    typeSpeed = this.waitTime;
    this.isDelete = true;
  } else if (this.isDelete && this.txt == "") {
    this.isDelete = false;
    this.wordIndex += 1;
    typeSpeed = 500;
  }

  setTimeout(() => this.typeFunc(), typeSpeed);
};

//initialize object
document.addEventListener("DOMContentLoaded", init);

function init() {
  const elements = document.querySelector(".txt-words");
  const words = JSON.parse(elements.getAttribute("data-words"));
  console.log(words);
  const waitTime = elements.getAttribute("data-period");
  new TyperWriter(elements, words, waitTime);
}


