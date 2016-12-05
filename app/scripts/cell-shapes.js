const Colours = {
  red: '#C0392b',
  blue: '#3498DB',
  green: '#2ECC71',
  grey: '#BDC3C7',
  textBackground: 'rgba(255, 255, 255, 0.5)'
};

/**
 * Styling for the default joint.js shapes.
 */
joint.shapes.fsa.EndState = joint.dia.Element.extend({

  markup: '<g class="rotatable"><g class="scalable"><circle class="outer"/><circle class="inner"/></g></g>',

  defaults: _.defaultsDeep({

    type: 'fsa.EndState',
    size: { width: 60, height: 60 },
    attrs: {
      '.outer': {
        'stroke-width': 3,
        transform: 'translate(10, 10)',
        r: 10,
        fill: '#ffffff',
        stroke: '#000000'
      },

      '.inner': {
        'stroke-width': 3,
        transform: 'translate(10, 10)',
        r: 6,
        fill: Colours.grey,
        stroke:'#000000'
      }
    }

  }, joint.dia.Element.prototype.defaults)
});

//Modify the transition element to place the label inside and not above.

joint.shapes.pn.Transition = joint.shapes.basic.Generic.extend({

  markup: '<g class="rotatable"><g class="scalable"><rect class="root"/></g></g><text class="label"/>',

  defaults: _.defaultsDeep({

    type: 'pn.Transition',
    size: { width: 60, height: 60 },
    attrs: {
      'rect': {
        width: 60,
        height: 60,
        fill: '#000000',
        stroke: '#000000'
      },
      '.label': {
        'text-anchor': 'middle',
        'ref-x': .5,
        'ref-y': 10,
        ref: 'rect',
        fill: '#FFFFFF',
        'font-size': 12
      }
    }

  }, joint.shapes.basic.Generic.prototype.defaults)
});

joint.shapes.pn.StartPlace = joint.dia.Element.extend({

  markup: '<g class="rotatable"><g class="scalable"><circle class="outer"/><circle class="inner"/></g></g>',

  defaults: _.defaultsDeep({

    type: 'pn.StartPlace',
    size: { width: 60, height: 60 },
    attrs: {
      '.outer': {
        'stroke-width': 3,
        transform: 'translate(10, 10)',
        r: 10,
        fill: Colours.grey,
        stroke: '#000000'
      },

      '.inner': {
        'stroke-width': 3,
        transform: 'translate(10, 10)',
        r: 3,
        fill: '#000000'
      }
    }

  }, joint.dia.Element.prototype.defaults)
});

joint.shapes.pn.Place = joint.shapes.pn.Place.extend({
  defaults: _.defaultsDeep({
    size: {width: 60, height: 60},
    attrs: {
      '.root': { fill:Colours.grey, 'stroke-width':3 },
      '.label': {text: '', fill: '#7c68fc'}
    }
  }, joint.shapes.pn.Place.prototype.defaults)
});

joint.shapes.pn.TerminalPlace = joint.dia.Element.extend({

  markup: '<g class="rotatable"><g class="scalable"><circle class="outer"/><circle class="inner"/></g></g>',

  defaults: _.defaultsDeep({

    type: 'pn.TerminalPlace',
    size: { width: 60, height: 60 },
    attrs: {
      '.outer': {
        'stroke-width': 3,
        transform: 'translate(10, 10)',
        r: 10,
        fill: 'green',
        stroke: '#000000'
      },

      '.inner': {
        'stroke-width': 3,
        transform: 'translate(10, 10)',
        r: 6,
        fill: Colours.grey,
        stroke:'#000000'
      }
    }

  }, joint.dia.Element.prototype.defaults)
});

joint.shapes.parent = joint.shapes.basic.Rect.extend();
joint.shapes.box = joint.shapes.basic.Rect.extend({

  markup: '<g class="rotatable"><g class="scalable"><rect/></g><text/></g>',

  defaults: _.defaultsDeep({

    type: 'box',
    attrs: {
      'rect': {
        fill: '#ffffff',
        stroke: '#000000',
        width: 100,
        height: 60
      },
      'text': {
        fill: '#000000',
        text: '',
        'font-size': 14,
        'ref-x': .5,
        'ref-y': .5,
        'text-anchor': 'middle',
        'y-alignment': 'middle',
        'font-family': 'Arial, helvetica, sans-serif'
      }
    }

  }, joint.shapes.basic.Generic.prototype.defaults)
});
/**
 * Get a cell's jquery object
 * @param cell the cell
 * @returns {jQuery|HTMLElement}
 */
function get$Cell(cell) {
  return $("[model-id='" + cell.id + "']");
}
