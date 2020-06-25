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
// typing main function
TyperWriter.prototype.typeFunc = function () {
  console.log(this.words.length);
  const currentIndex = this.wordIndex % this.words.length;
  const fullWord = this.words[currentIndex];

  if (this.isDelete) {
    // Remove character
    this.txt = fullWord.substring(0, this.txt.length - 1);
  } else {
    // Add character
    this.txt = fullWord.substring(0, this.txt.length + 1);
  }

  // Output on website
  this.elements.textContent = this.txt;
  let typeSpeed = this.isDelete ? 50 : 100;
  // Change type speed when deleting
  if (!this.isDelete && this.txt === fullWord) {
    typeSpeed = this.waitTime;
    this.isDelete = true;
  } else if (this.isDelete && this.txt === "") {
    this.isDelete = false;
    this.wordIndex += 1;
    typeSpeed = 500;
  }

  setTimeout(() => this.typeFunc(), typeSpeed);
};

document.addEventListener("DOMContentLoaded", init);

function init() {
  const elements = document.getElementById("text");
  const words = [
    "— a student at Massachusetts Institute of Technology.",
    "— a matcha addict.",
    "— also a coffee addict.",
    "— a San Jose native.",
  ];
  const waitTime = elements.getAttribute("data-period");
  new TyperWriter(elements, words, waitTime);
}
